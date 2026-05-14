package com.contrastsecurity.cassandra.migration.config;

import com.contrastsecurity.cassandra.migration.CassandraMigrationException;

public final class ScriptsLocation implements Comparable<ScriptsLocation> {

    private static final String CLASSPATH_PREFIX = "classpath:";
    public static final String FILESYSTEM_PREFIX = "filesystem:";

    private final String prefix; //classpath or filesystem
    private final String path;

    public ScriptsLocation(String descriptor) {
        String normalizedDescriptor = descriptor.trim().replace("\\", "/");

        String tempPath;
        if (normalizedDescriptor.contains(":")) {
            prefix = normalizedDescriptor.substring(0, normalizedDescriptor.indexOf(":") + 1);
            tempPath = normalizedDescriptor.substring(normalizedDescriptor.indexOf(":") + 1);
        } else {
            prefix = CLASSPATH_PREFIX;
            tempPath = normalizedDescriptor;
        }

        if (isClassPath()) {
            tempPath = tempPath.replace(".", "/");
            if (tempPath.startsWith("/")) {
                tempPath = tempPath.substring(1);
            }
        } else {
            if (!isFileSystem()) {
                throw new CassandraMigrationException("Unknown prefix for location. " +
                        "Must be " + CLASSPATH_PREFIX + " or " + FILESYSTEM_PREFIX + "."
                        + normalizedDescriptor);
            }
        }

        if (tempPath.endsWith("/")) {
            tempPath = tempPath.substring(0, tempPath.length() - 1);
        }
        path = tempPath;
    }

    public boolean isClassPath() {
        return CLASSPATH_PREFIX.equals(prefix);
    }

    public boolean isFileSystem() {
        return FILESYSTEM_PREFIX.equals(prefix);
    }

    public boolean isParentOf(ScriptsLocation other) {
        return (other.getDescriptor() + "/").startsWith(getDescriptor() + "/");
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPath() {
        return path;
    }

    public String getDescriptor() {
        return prefix + path;
    }

    public int compareTo(ScriptsLocation o) {
        return getDescriptor().compareTo(o.getDescriptor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptsLocation location = (ScriptsLocation) o;

        return getDescriptor().equals(location.getDescriptor());
    }

    @Override
    public int hashCode() {
        return getDescriptor().hashCode();
    }

    @Override
    public String toString() {
        return getDescriptor();
    }
}
