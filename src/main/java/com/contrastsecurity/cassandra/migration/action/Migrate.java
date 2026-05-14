package com.contrastsecurity.cassandra.migration.action;

import com.contrastsecurity.cassandra.migration.CassandraMigrationException;
import com.contrastsecurity.cassandra.migration.dao.SchemaVersionDAO;
import com.contrastsecurity.cassandra.migration.info.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.contrastsecurity.cassandra.migration.resolver.MigrationExecutor;
import com.contrastsecurity.cassandra.migration.resolver.MigrationResolver;
import com.contrastsecurity.cassandra.migration.utils.StopWatch;
import com.contrastsecurity.cassandra.migration.utils.TimeFormat;
import com.datastax.oss.driver.api.core.CqlSession;

public class Migrate {
    private static final Logger LOG = LoggerFactory.getLogger(Migrate.class);

    private final MigrationVersion target;
    private final SchemaVersionDAO schemaVersionDAO;
    private final MigrationResolver migrationResolver;
    private final CqlSession session;
    private final String user;
    private final boolean allowOutOfOrder;

    public Migrate(MigrationResolver migrationResolver, MigrationVersion target, SchemaVersionDAO schemaVersionDAO,
                   CqlSession session, String user, boolean allowOutOfOrder) {
        this.migrationResolver = migrationResolver;
        this.schemaVersionDAO = schemaVersionDAO;
        this.session = session;
        this.target = target;
        this.user = user;
        this.allowOutOfOrder = allowOutOfOrder;
    }

    public int run() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int migrationSuccessCount = 0;
        while (true) {
            final boolean firstRun = migrationSuccessCount == 0;

            MigrationInfoService infoService = new MigrationInfoService(migrationResolver, schemaVersionDAO, target, allowOutOfOrder, true);
            infoService.refresh();

            MigrationVersion currentSchemaVersion = MigrationVersion.EMPTY;
            if (infoService.current() != null) {
                currentSchemaVersion = infoService.current().getVersion();
            }
            if (firstRun) {
                LOG.info("Current version of keyspace {}: {}", schemaVersionDAO.getKeyspace().getName(), currentSchemaVersion);
            }

            MigrationInfo[] future = infoService.future();
            if (future.length > 0) {
                MigrationInfo[] resolved = infoService.resolved();
                if (resolved.length == 0) {
                    LOG.warn("Keyspace {} has version {}, but no migration could be resolved in the configured locations !", schemaVersionDAO.getKeyspace().getName(), currentSchemaVersion);
                } else {
                    LOG.warn("Keyspace {} has a version ({}) that is newer than the latest available migration ({}) !", schemaVersionDAO.getKeyspace().getName(), currentSchemaVersion, resolved[resolved.length - 1].getVersion());
                }
            }

            MigrationInfo[] failed = infoService.failed();
            if (failed.length > 0) {
                if ((failed.length == 1)
                        && (failed[0].getState() == MigrationState.FUTURE_FAILED)) {
                    LOG.warn("Keyspace {} contains a failed future migration to version {} !", schemaVersionDAO.getKeyspace().getName(), failed[0].getVersion());
                } else {
                    throw new CassandraMigrationException("Keyspace " + schemaVersionDAO.getKeyspace().getName() + " contains a failed migration to version " + failed[0].getVersion() + " !");
                }
            }

            MigrationInfo[] pendingMigrations = infoService.pending();

            if (pendingMigrations.length == 0) {
                break;
            }

            boolean isOutOfOrder = pendingMigrations[0].getVersion().compareTo(currentSchemaVersion) < 0;
            MigrationVersion mv = applyMigration(pendingMigrations[0], isOutOfOrder);
            if(mv == null) {
                //no more migrations
                break;
            }

            migrationSuccessCount++;
        }

        stopWatch.stop();

        logSummary(migrationSuccessCount, stopWatch.getTotalTimeMillis());

        return migrationSuccessCount;
    }

    private MigrationVersion applyMigration(final MigrationInfo migration, boolean isOutOfOrder) {
        MigrationVersion version = migration.getVersion();
        LOG.info("Migrating keyspace {} to version {} - {}{}", schemaVersionDAO.getKeyspace().getName(), version, migration.getDescription(), isOutOfOrder ? " (out of order)" : "");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            final MigrationExecutor migrationExecutor = migration.getResolvedMigration().getExecutor();
            try {
                migrationExecutor.execute(session);
            } catch (Exception e) {
                throw new CassandraMigrationException("Unable to apply migration", e);
            }
            LOG.debug("Successfully completed and committed migration of keyspace {} to version {}", schemaVersionDAO.getKeyspace().getName(), version);
        } catch (CassandraMigrationException e) {
            LOG.error("Migration of keyspace {} to version {} failed! Please restore backups and roll back database and code!", schemaVersionDAO.getKeyspace().getName(), version);

            stopWatch.stop();
            int executionTime = (int) stopWatch.getTotalTimeMillis();
            AppliedMigration appliedMigration = new AppliedMigration(version, migration.getDescription(),
                    migration.getType(), migration.getScript(), migration.getChecksum(), user, executionTime, false);
            schemaVersionDAO.addAppliedMigration(appliedMigration);
            throw e;
        }

        stopWatch.stop();
        int executionTime = (int) stopWatch.getTotalTimeMillis();

        AppliedMigration appliedMigration = new AppliedMigration(version, migration.getDescription(),
                migration.getType(), migration.getScript(), migration.getChecksum(), user, executionTime, true);
        schemaVersionDAO.addAppliedMigration(appliedMigration);

        return version;
    }

    /**
     * Logs the summary of this migration run.
     *
     * @param migrationSuccessCount The number of successfully applied migrations.
     * @param executionTime         The total time taken to perform this migration run (in ms).
     */
    private void logSummary(int migrationSuccessCount, long executionTime) {
        if (migrationSuccessCount == 0) {
            LOG.info("Keyspace {} is up to date. No migration necessary.", schemaVersionDAO.getKeyspace().getName());
            return;
        }

        if (migrationSuccessCount == 1) {
            LOG.info("Successfully applied 1 migration to keyspace {} (execution time {}).", schemaVersionDAO.getKeyspace().getName(), TimeFormat.format(executionTime));
        } else {
            LOG.info("Successfully applied {} migrations to keyspace {} (execution time {}).", migrationSuccessCount, schemaVersionDAO.getKeyspace().getName(), TimeFormat.format(executionTime));
        }
    }
}
