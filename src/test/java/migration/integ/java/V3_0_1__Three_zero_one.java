package migration.integ.java;

import com.contrastsecurity.cassandra.migration.api.JavaMigration;
import com.datastax.oss.driver.api.core.CqlSession;

public class V3_0_1__Three_zero_one implements JavaMigration {
    @Override
    public void migrate(CqlSession session) throws Exception {
        session.execute("INSERT INTO test1 (space, key, value) VALUES ('web', 'facebook', 'facebook.com')");
    }
}
