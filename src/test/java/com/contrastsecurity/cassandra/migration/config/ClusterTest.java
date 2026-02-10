package com.contrastsecurity.cassandra.migration.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterTest {
    @Test
    public void shouldHaveDefaultConfigValues() {
        Cluster cluster = new Cluster();
        assertThat(cluster.getContactPoints()[0]).isEqualTo("localhost");
        assertThat(cluster.getPort()).isEqualTo(9042);
        assertThat(cluster.getUsername()).isNull();
        assertThat(cluster.getPassword()).isNull();
    }

    @Test
    public void systemPropsShouldOverrideDefaultConfigValues() {
        System.setProperty(Cluster.ClusterProperty.CONTACTPOINTS.getName(), "192.168.0.1,192.168.0.2, 192.168.0.3");
        System.setProperty(Cluster.ClusterProperty.PORT.getName(), "9144");
        System.setProperty(Cluster.ClusterProperty.USERNAME.getName(), "user");
        System.setProperty(Cluster.ClusterProperty.PASSWORD.getName(), "pass");

        Cluster cluster = new Cluster();
        assertThat(cluster.getContactPoints()[0]).isEqualTo("192.168.0.1");
        assertThat(cluster.getContactPoints()[1]).isEqualTo("192.168.0.2");
        assertThat(cluster.getContactPoints()[2]).isEqualTo("192.168.0.3");
        assertThat(cluster.getPort()).isEqualTo(9144);
        assertThat(cluster.getUsername()).isEqualTo("user");
        assertThat(cluster.getPassword()).isEqualTo("pass");
    }
}
