package com.contrastsecurity.cassandra.migration;

import com.contrastsecurity.cassandra.migration.action.Initialize;
import com.contrastsecurity.cassandra.migration.action.Migrate;
import com.contrastsecurity.cassandra.migration.action.Validate;
import com.contrastsecurity.cassandra.migration.config.Keyspace;
import com.contrastsecurity.cassandra.migration.config.MigrationConfigs;
import com.contrastsecurity.cassandra.migration.config.ScriptsLocations;
import com.contrastsecurity.cassandra.migration.dao.SchemaVersionDAO;
import com.contrastsecurity.cassandra.migration.info.MigrationInfoService;
import com.contrastsecurity.cassandra.migration.info.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.contrastsecurity.cassandra.migration.resolver.CompositeMigrationResolver;
import com.contrastsecurity.cassandra.migration.resolver.MigrationResolver;
import com.contrastsecurity.cassandra.migration.utils.VersionPrinter;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CassandraMigration {

    private static final Logger LOG = LoggerFactory.getLogger(CassandraMigration.class);

    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Keyspace keyspace;
    private MigrationConfigs configs;

    public CassandraMigration() {
        this.keyspace = new Keyspace();
        this.configs = new MigrationConfigs();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the ClassLoader to use for resolving migrations on the classpath.
     *
     * @param classLoader The ClassLoader to use for resolving migrations on the classpath. (default: Thread.currentThread().getContextClassLoader() )
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Keyspace getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    public MigrationConfigs getConfigs() {
        return configs;
    }

    private MigrationResolver createMigrationResolver() {
        return new CompositeMigrationResolver(classLoader, new ScriptsLocations(configs.getScriptsLocations()), configs.getEncoding());
    }

    public int migrate(CqlSession session) {
        return execute(new Action<Integer>() {

            @Override
            public Optional<CqlSession> getCqlSession() {
                return Optional.ofNullable(session);
            }

            public Integer execute(CqlSession session) {
                new Initialize().run(session, keyspace, MigrationVersion.CURRENT.getTable());

                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.getTable());
                Migrate migrate = new Migrate(migrationResolver, configs.getTarget(), schemaVersionDAO, session,
                        keyspace.getCluster().getUsername(), configs.isAllowOutOfOrder());

                return migrate.run();
            }
        });
    }

    public int migrate() {
        return execute(new Action<Integer>() {
            public Integer execute(CqlSession session) {
                new Initialize().run(session, keyspace, MigrationVersion.CURRENT.getTable());

                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.getTable());
                Migrate migrate = new Migrate(migrationResolver, configs.getTarget(), schemaVersionDAO, session,
                        keyspace.getCluster().getUsername(), configs.isAllowOutOfOrder());

                return migrate.run();
            }
        });
    }

    public MigrationInfoService info() {
        return execute(new Action<MigrationInfoService>() {
            public MigrationInfoService execute(CqlSession session) {
                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDAO = new SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.getTable());
                MigrationInfoService migrationInfoService =
                        new MigrationInfoService(migrationResolver, schemaVersionDAO, configs.getTarget(), false, true);
                migrationInfoService.refresh();

                return migrationInfoService;
            }
        });
    }

    public void validate() {
        String validationError = execute(new Action<String>() {
            @Override
            public String execute(CqlSession session) {
                MigrationResolver migrationResolver = createMigrationResolver();
                SchemaVersionDAO schemaVersionDao = new SchemaVersionDAO(session, keyspace, MigrationVersion.CURRENT.getTable());
                Validate validate = new Validate(migrationResolver, schemaVersionDao, configs.getTarget(), true, false);
                return validate.run();
            }
        });

        if (validationError != null) {
            throw new CassandraMigrationException("Validation failed. " + validationError);
        }
    }

    public void baseline() {
        //TODO
        throw new UnsupportedOperationException();
    }

    private String getConnectionInfo(Metadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("Connected to cluster: ");
        sb.append(metadata.getClusterName().orElse("N/A"));
        sb.append("\n");
        for (Node node : metadata.getNodes().values()) {
            sb.append("Data center: ");
            sb.append(node.getDatacenter());
            sb.append("; Host: ");
            sb.append(node.getEndPoint());
        }
        return sb.toString();
    }

    <T> T execute(Action<T> action) {
        T result;

        VersionPrinter.printVersion(classLoader);

        CqlSession session = null;
        try {
            if (null == keyspace)
                throw new IllegalArgumentException("Unable to establish Cassandra session. Keyspace is not configured.");

            if (null == keyspace.getCluster())
                throw new IllegalArgumentException("Unable to establish Cassandra session. Cluster is not configured.");

            session = action.getCqlSession()
                    .orElseGet(() -> {
                        CqlSessionBuilder builder = CqlSession.builder();
                        List<InetSocketAddress> contactPoints = new ArrayList<>();
                        for (String cp : keyspace.getCluster().getContactPoints()) {
                            contactPoints.add(new InetSocketAddress(cp, keyspace.getCluster().getPort()));
                        }
                        builder.addContactPoints(contactPoints);
                        builder.withLocalDatacenter("datacenter1"); // Default, should be configurable

                        if (null != keyspace.getCluster().getUsername() && !keyspace.getCluster().getUsername().trim().isEmpty()) {
                            if (null != keyspace.getCluster().getPassword() && !keyspace.getCluster().getPassword().trim().isEmpty()) {
                                builder.withAuthCredentials(keyspace.getCluster().getUsername(),
                                        keyspace.getCluster().getPassword());
                            } else {
                                throw new IllegalArgumentException("Password must be provided with username.");
                            }
                        }
                        return builder.build();
                    });

            Metadata metadata = session.getMetadata();
            LOG.info("{}", getConnectionInfo(metadata));

            if (null == keyspace.getName() || keyspace.getName().trim().length() == 0)
                throw new IllegalArgumentException("Keyspace not specified.");

            if (metadata.getKeyspace(keyspace.getName()).isPresent())
                session.execute("USE " + keyspace.getName());
            else
                throw new CassandraMigrationException("Keyspace: " + keyspace.getName() + " does not exist.");

            result = action.execute(session);
        } finally {
            if (action.getCqlSession().isEmpty() && null != session && !session.isClosed())
                try {
                    session.close();
                } catch (Exception e) {
                    LOG.warn("Error closing Cassandra session");
                }
        }
        return result;
    }

    interface Action<T> {
        T execute(CqlSession session);

        default Optional<CqlSession> getCqlSession() {
            return Optional.empty();
        }

    }
}
