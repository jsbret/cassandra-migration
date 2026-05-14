package com.contrastsecurity.cassandra.migration.config;

public class Cluster {
    private static final String PROPERTY_PREFIX = "cassandra.migration.cluster.";

    public enum ClusterProperty {
        CONTACTPOINTS(PROPERTY_PREFIX + "contactpoints", "Comma separated values of node IP addresses"),
        PORT(PROPERTY_PREFIX + "port", "CQL native transport port"),
        USERNAME(PROPERTY_PREFIX + "username", "Username for password authenticator"),
        PASSWORD(PROPERTY_PREFIX + "password", "Password for password authenticator");

        private final String name;
        private final String description;

        ClusterProperty(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    private String[] contactpoints = {"localhost"};
    private int port = 9042;
    private String username;
    private String password;

    public Cluster() {
        String contactPoints = System.getProperty(ClusterProperty.CONTACTPOINTS.getName());
        if (null != contactPoints && !contactPoints.trim().isEmpty())
            this.contactpoints = contactPoints.replaceAll("\\s+", "").split("[,]");

        String port = System.getProperty(ClusterProperty.PORT.getName());
        if (null != port && !port.trim().isEmpty())
            this.port = Integer.parseInt(port);

        String username = System.getProperty(ClusterProperty.USERNAME.getName());
        if (null != username && !username.trim().isEmpty())
            this.username = username;

        String password = System.getProperty(ClusterProperty.PASSWORD.getName());
        if (null != password && !password.trim().isEmpty())
            this.password = password;
    }

    public String[] getContactPoints() {
        return contactpoints;
    }

    public void setContactPoints(String... contactPoints) {
        this.contactpoints = contactPoints;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
