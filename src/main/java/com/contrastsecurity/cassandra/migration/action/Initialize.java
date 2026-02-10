package com.contrastsecurity.cassandra.migration.action;

import com.contrastsecurity.cassandra.migration.config.Keyspace;
import com.contrastsecurity.cassandra.migration.dao.SchemaVersionDAO;
import com.datastax.oss.driver.api.core.CqlSession;

public class Initialize {

    public void run(CqlSession session, Keyspace keyspace, String migrationVersionTableName) {
        SchemaVersionDAO dao = new SchemaVersionDAO(session, keyspace, migrationVersionTableName);
        dao.createTablesIfNotExist();
    }
}
