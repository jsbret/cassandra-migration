package com.contrastsecurity.cassandra.migration;

import com.contrastsecurity.cassandra.migration.config.Keyspace;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultProgrammaticDriverConfigLoaderBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.CassandraContainer;

import java.net.InetSocketAddress;
import java.time.Duration;

public abstract class BaseIT {
    public static final String CASSANDRA__KEYSPACE = "cassandra_migration_test";
    public static CassandraContainer cassandra = new CassandraContainer("cassandra:4.1");

    public static String getContactPoint() {
        return cassandra.getHost();
    }

    public static int getPort() {
        return cassandra.getMappedPort(9042);
    }

    public static String getUsername() {
        return cassandra.getUsername();
    }

    public static String getPassword() {
        return cassandra.getPassword();
    }

    private CqlSession session;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        cassandra.start();
    }

    @AfterAll
    public static void afterSuite() {
        cassandra.stop();
    }

    @BeforeEach
    public void createKeyspace() {
        SimpleStatement statement = SimpleStatement.newInstance(
                "CREATE KEYSPACE " + CASSANDRA__KEYSPACE +
                        "  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };"
        );
        getSession(getKeyspace()).execute(statement);
    }

    @AfterEach
    public void dropKeyspace() {
        SimpleStatement statement = SimpleStatement.newInstance(
                "DROP KEYSPACE " + CASSANDRA__KEYSPACE + ";"
        );
        getSession(getKeyspace()).execute(statement);
    }

    protected Keyspace getKeyspace() {
        Keyspace ks = new Keyspace();
        ks.setName(CASSANDRA__KEYSPACE);
        ks.getCluster().setContactPoints(cassandra.getHost());
        ks.getCluster().setPort(cassandra.getMappedPort(9042));
        ks.getCluster().setUsername(cassandra.getUsername());
        ks.getCluster().setPassword(cassandra.getPassword());
        return ks;
    }

    private CqlSession getSession(Keyspace keyspace) {
        if (session != null && !session.isClosed())
            return session;

        DriverConfigLoader driverConfigLoader = new DefaultProgrammaticDriverConfigLoaderBuilder()
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(10))
                .build();

        session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(9042)))
                .withLocalDatacenter("datacenter1")
                .withAuthCredentials(keyspace.getCluster().getUsername(), keyspace.getCluster().getPassword())
                .withConfigLoader(driverConfigLoader)
                .build();
        return session;
    }

    protected CqlSession getSession() {
        return session;
    }
}
