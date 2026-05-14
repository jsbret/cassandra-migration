package com.contrastsecurity.cassandra.migration.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeyspaceTest {
    @Test
    public void shouldDefaultToNoKeyspaceButCanBeOverridden() {
        assertThat(new Keyspace().getName()).isNull();
        System.setProperty(Keyspace.KeyspaceProperty.NAME.getName(), "myspace");
        assertThat(new Keyspace().getName()).isEqualTo("myspace");
    }

    @Test
    public void shouldHaveDefaultClusterObject() {
        assertThat(new Keyspace().getCluster()).isNotNull();
    }
}
