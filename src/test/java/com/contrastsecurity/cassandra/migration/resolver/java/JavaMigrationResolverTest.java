/**
 * Copyright 2010-2015 Axel Fontaine
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrastsecurity.cassandra.migration.resolver.java;

import com.contrastsecurity.cassandra.migration.CassandraMigrationException;
import com.contrastsecurity.cassandra.migration.config.ScriptsLocation;
import com.contrastsecurity.cassandra.migration.info.ResolvedMigration;
import com.contrastsecurity.cassandra.migration.resolver.java.dummy.V2__InterfaceBasedMigration;
import com.contrastsecurity.cassandra.migration.resolver.java.dummy.Version3dot5;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test for JavaMigrationResolver.
 */
public class JavaMigrationResolverTest {
    @Test
    public void broken() {
        assertThrows(CassandraMigrationException.class, () -> new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), new ScriptsLocation("com/contrastsecurity/cassandra/migration/resolver/java/error")).resolveMigrations());
    }

    @Test
    public void resolveMigrations() {
        JavaMigrationResolver jdbcMigrationResolver =
                new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), new ScriptsLocation("com/contrastsecurity/cassandra/migration/resolver/java/dummy"));
        Collection<ResolvedMigration> migrations = jdbcMigrationResolver.resolveMigrations();

        assertThat(migrations).hasSize(3);

        List<ResolvedMigration> migrationList = new ArrayList<ResolvedMigration>(migrations);

        ResolvedMigration migrationInfo = migrationList.get(0);
        assertThat(migrationInfo.getVersion().toString()).isEqualTo("2");
        assertThat(migrationInfo.getDescription()).isEqualTo("InterfaceBasedMigration");
        assertThat(migrationInfo.getChecksum()).isNull();

        ResolvedMigration migrationInfo1 = migrationList.get(1);
        assertThat(migrationInfo1.getVersion().toString()).isEqualTo("3.5");
        assertThat(migrationInfo1.getDescription()).isEqualTo("Three Dot Five");
        assertThat(migrationInfo1.getChecksum().intValue()).isEqualTo(35);

        ResolvedMigration migrationInfo2 = migrationList.get(2);
        assertThat(migrationInfo2.getVersion().toString()).isEqualTo("4");
    }

    @Test
    public void conventionOverConfiguration() {
        JavaMigrationResolver jdbcMigrationResolver = new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), null);
        ResolvedMigration migrationInfo = jdbcMigrationResolver.extractMigrationInfo(new V2__InterfaceBasedMigration());
        assertThat(migrationInfo.getVersion().toString()).isEqualTo("2");
        assertThat(migrationInfo.getDescription()).isEqualTo("InterfaceBasedMigration");
        assertThat(migrationInfo.getChecksum()).isNull();
    }

    @Test
    public void explicitInfo() {
        JavaMigrationResolver jdbcMigrationResolver = new JavaMigrationResolver(Thread.currentThread().getContextClassLoader(), null);
        ResolvedMigration migrationInfo = jdbcMigrationResolver.extractMigrationInfo(new Version3dot5());
        assertThat(migrationInfo.getVersion().toString()).isEqualTo("3.5");
        assertThat(migrationInfo.getDescription()).isEqualTo("Three Dot Five");
        assertThat(migrationInfo.getChecksum().intValue()).isEqualTo(35);
    }
}
