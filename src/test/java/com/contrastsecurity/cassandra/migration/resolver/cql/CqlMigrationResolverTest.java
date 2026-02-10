package com.contrastsecurity.cassandra.migration.resolver.cql;

import com.contrastsecurity.cassandra.migration.CassandraMigrationException;
import com.contrastsecurity.cassandra.migration.config.ScriptsLocation;
import com.contrastsecurity.cassandra.migration.info.ResolvedMigration;
import com.contrastsecurity.cassandra.migration.utils.scanner.classpath.ClassPathResource;
import com.contrastsecurity.cassandra.migration.utils.scanner.filesystem.FileSystemResource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testcase for CqlMigration.
 */
public class CqlMigrationResolverTest {
    @Test
    public void resolveMigrations() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("migration/subdir"), "UTF-8");
        Collection<ResolvedMigration> migrations = cqlMigrationResolver.resolveMigrations();

        assertThat(migrations).hasSize(3);

        List<ResolvedMigration> migrationList = new ArrayList<ResolvedMigration>(migrations);

        assertThat(migrationList.get(0).getVersion().toString()).isEqualTo("1");
        assertThat(migrationList.get(1).getVersion().toString()).isEqualTo("1.1");
        assertThat(migrationList.get(2).getVersion().toString()).isEqualTo("2.0");

        assertThat(migrationList.get(0).getScript()).isEqualTo("dir1/V1__First.cql");
        assertThat(migrationList.get(1).getScript()).isEqualTo("V1_1__Populate_table.cql");
        assertThat(migrationList.get(2).getScript()).isEqualTo("dir2/V2_0__Add_contents_table.cql");
    }

    @Test
    public void resolveMigrationsNonExisting() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("non/existing"), "UTF-8");

        assertThrows(CassandraMigrationException.class, () -> cqlMigrationResolver.resolveMigrations());
    }

    @Test
    public void extractScriptName() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("db/migration"), "UTF-8");

        assertThat(cqlMigrationResolver.extractScriptName(
                new ClassPathResource("db/migration/db_0__init.cql", Thread.currentThread().getContextClassLoader()))).isEqualTo("db_0__init.cql");
    }

    @Test
    public void extractScriptNameRootLocation() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(), new ScriptsLocation(""), "UTF-8");

        assertThat(cqlMigrationResolver.extractScriptName(
                new ClassPathResource("db_0__init.cql", Thread.currentThread().getContextClassLoader()))).isEqualTo("db_0__init.cql");
    }

    @Test
    public void extractScriptNameFileSystemPrefix() {
        CqlMigrationResolver cqlMigrationResolver =
                new CqlMigrationResolver(Thread.currentThread().getContextClassLoader(),
                        new ScriptsLocation("filesystem:/some/dir"), "UTF-8");

        assertThat(cqlMigrationResolver.extractScriptName(new FileSystemResource("/some/dir/V3.171__patch.cql"))).isEqualTo("V3.171__patch.cql");
    }
}
