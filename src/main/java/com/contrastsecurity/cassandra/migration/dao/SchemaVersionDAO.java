package com.contrastsecurity.cassandra.migration.dao;

import com.contrastsecurity.cassandra.migration.config.Keyspace;
import com.contrastsecurity.cassandra.migration.config.MigrationType;
import com.contrastsecurity.cassandra.migration.info.AppliedMigration;
import com.contrastsecurity.cassandra.migration.info.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.contrastsecurity.cassandra.migration.utils.CachePrepareStatement;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;

import java.util.*;

public class SchemaVersionDAO {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaVersionDAO.class);
    private static final String COUNTS_TABLE_NAME_SUFFIX = "_counts";

    private final CqlSession session;
    private final Keyspace keyspace;
    private final String tableName;
    private final CachePrepareStatement cachePs;
    private final ConsistencyLevel consistencyLevel;

    public SchemaVersionDAO(CqlSession session, Keyspace keyspace, String tableName) {
        this.session = session;
        this.keyspace = keyspace;
        this.tableName = tableName;
        this.cachePs = new CachePrepareStatement(session);
        //If running on a single host, don't force ConsistencyLevel.ALL
        this.consistencyLevel = ConsistencyLevel.ALL;
    }

    public Keyspace getKeyspace() {
        return this.keyspace;
    }

    public void createTablesIfNotExist() {
        if (tablesExist()) {
            return;
        }

        SimpleStatement statement = SimpleStatement.newInstance(
                "CREATE TABLE IF NOT EXISTS " + keyspace.getName() + "." + tableName + "(" +
                        "  version_rank int," +
                        "  installed_rank int," +
                        "  version text," +
                        "  description text," +
                        "  script text," +
                        "  checksum int," +
                        "  type text," +
                        "  installed_by text," +
                        "  installed_on timestamp," +
                        "  execution_time int," +
                        "  success boolean," +
                        "  PRIMARY KEY (version)" +
                        ");");
        session.execute(statement.setConsistencyLevel(this.consistencyLevel));

        statement = SimpleStatement.newInstance(
                "CREATE TABLE IF NOT EXISTS " + keyspace.getName() + "." + tableName + COUNTS_TABLE_NAME_SUFFIX + " (" +
                        "  name text," +
                        "  count counter," +
                        "  PRIMARY KEY (name)" +
                        ");");
        session.execute(statement.setConsistencyLevel(this.consistencyLevel));
    }

    public boolean tablesExist() {
        boolean schemaVersionTableExists = false;
        boolean schemaVersionCountsTableExists = false;

        SimpleStatement schemaVersionStatement = SimpleStatement.newInstance(
                "SELECT count(*) FROM " + keyspace.getName() + "." + tableName);

        SimpleStatement schemaVersionCountsStatement = SimpleStatement.newInstance(
                "SELECT count(*) FROM " + keyspace.getName() + "." + tableName + COUNTS_TABLE_NAME_SUFFIX);

        try {
            ResultSet resultsSchemaVersion = session.execute(schemaVersionStatement.setConsistencyLevel(this.consistencyLevel));
            if (resultsSchemaVersion.one() != null) {
                schemaVersionTableExists = true;
            }
        } catch (InvalidQueryException e) {
            LOG.debug("No schema version table found with a name of {}", tableName);
        }

        try {
            ResultSet resultsSchemaVersionCounts = session.execute(schemaVersionCountsStatement.setConsistencyLevel(this.consistencyLevel));
            if (resultsSchemaVersionCounts.one() != null) {
                schemaVersionCountsTableExists = true;
            }
        } catch (InvalidQueryException e) {
            LOG.debug("No schema version counts table found with a name of {}{}", tableName, COUNTS_TABLE_NAME_SUFFIX);
        }

        return schemaVersionTableExists && schemaVersionCountsTableExists;
    }

    public void addAppliedMigration(AppliedMigration appliedMigration) {
        createTablesIfNotExist();

        MigrationVersion version = appliedMigration.getVersion();

        int versionRank = calculateVersionRank(version);
        PreparedStatement statement = cachePs.prepare(
                "INSERT INTO " + keyspace.getName() + "." + tableName +
                        " (version_rank, installed_rank, version, description, type, script, checksum, installed_on," +
                        "  installed_by, execution_time, success)" +
                        " VALUES" +
                        " (?, ?, ?, ?, ?, ?, ?, toTimestamp(now()), ?, ?, ?);"
        );

        session.execute(statement.bind(
                versionRank,
                calculateInstalledRank(),
                version.toString(),
                appliedMigration.getDescription(),
                appliedMigration.getType().name(),
                appliedMigration.getScript(),
                appliedMigration.getChecksum(),
                appliedMigration.getInstalledBy(),
                appliedMigration.getExecutionTime(),
                appliedMigration.isSuccess()
        ).setConsistencyLevel(this.consistencyLevel));
        LOG.debug("Schema version table {} successfully updated to reflect changes", tableName);
    }

    /**
     * Retrieve the applied migrations from the metadata table.
     *
     * @return The applied migrations.
     */
    public List<AppliedMigration> findAppliedMigrations() {
        if (!tablesExist()) {
            return new ArrayList<>();
        }

        SimpleStatement statement = SimpleStatement.newInstance(
                "SELECT version_rank, installed_rank, version, description, type, script, checksum, installed_on, installed_by, execution_time, success" +
                        " FROM " + keyspace.getName() + "." + tableName);

        ResultSet results = session.execute(statement.setConsistencyLevel(this.consistencyLevel));
        List<AppliedMigration> resultsList = new ArrayList<>();
        for (Row row : results) {
            resultsList.add(new AppliedMigration(
                    row.getInt("version_rank"),
                    row.getInt("installed_rank"),
                    MigrationVersion.fromVersion(row.getString("version")),
                    row.getString("description"),
                    MigrationType.valueOf(row.getString("type")),
                    row.getString("script"),
                    row.isNull("checksum") ? null : row.getInt("checksum"),
                    row.getInstant("installed_on") != null ? java.util.Date.from(Objects.requireNonNull(row.getInstant("installed_on"))) : null,
                    row.getString("installed_by"),
                    row.getInt("execution_time"),
                    row.getBoolean("success")
            ));
        }

        //order by version_rank not necessary here as it eventually gets saved in TreeMap that uses natural ordering

        return resultsList;
    }

    /**
     * Calculates the installed rank for the new migration to be inserted.
     *
     * @return The installed rank.
     */
    private int calculateInstalledRank() {
        SimpleStatement updateStatement = SimpleStatement.newInstance(
                "UPDATE " + keyspace.getName() + "." + tableName + COUNTS_TABLE_NAME_SUFFIX +
                        " SET count = count + 1 WHERE name = 'installed_rank'");
        session.execute(updateStatement.setConsistencyLevel(this.consistencyLevel));

        SimpleStatement selectStatement = SimpleStatement.newInstance(
                "SELECT count FROM " + keyspace.getName() + "." + tableName + COUNTS_TABLE_NAME_SUFFIX +
                        " WHERE name = 'installed_rank'");
        ResultSet result = session.execute(selectStatement.setConsistencyLevel(this.consistencyLevel));
        Row row = result.one();
        return (row != null) ? (int) row.getLong("count") : 0;
    }

    record MigrationMetaHolder(int versionRank) {
    }

    /**
     * Calculate the rank for this new version about to be inserted.
     *
     * @param version The version to calculated for.
     * @return The rank.
     */
    private int calculateVersionRank(MigrationVersion version) {
        SimpleStatement statement = SimpleStatement.newInstance(
                "SELECT version, version_rank FROM " + keyspace.getName() + "." + tableName);

        ResultSet versionRows = session.execute(statement.setConsistencyLevel(this.consistencyLevel));

        List<MigrationVersion> migrationVersions = new ArrayList<>();
        HashMap<String, MigrationMetaHolder> migrationMetaHolders = new HashMap<>();
        for (Row versionRow : versionRows) {
            String versionStr = versionRow.getString("version");
            MigrationVersion migrationVersion = MigrationVersion.fromVersion(versionStr);
            migrationVersions.add(migrationVersion);
            migrationMetaHolders.put(versionStr, new MigrationMetaHolder(versionRow.getInt("version_rank")));
        }

        Collections.sort(migrationVersions);

        BatchStatement batchStatement = BatchStatement.newInstance(BatchType.UNLOGGED);
        PreparedStatement preparedStatement = cachePs.prepare(
                "UPDATE " + keyspace.getName() + "." + tableName +
                        " SET version_rank = ?" +
                        " WHERE version = ?;");

        for (int i = 0; i < migrationVersions.size(); i++) {
            if (version.compareTo(migrationVersions.get(i)) < 0) {
                for (int z = i; z < migrationVersions.size(); z++) {
                    String migrationVersionStr = migrationVersions.get(z).getVersion();
                    batchStatement = batchStatement.add(preparedStatement.bind(
                            migrationMetaHolders.get(migrationVersionStr).versionRank() + 1,
                            migrationVersionStr
                    ));
                }
                session.execute(batchStatement.setConsistencyLevel(this.consistencyLevel));
                return i + 1;
            }
        }
        return migrationVersions.size() + 1;
    }
}
