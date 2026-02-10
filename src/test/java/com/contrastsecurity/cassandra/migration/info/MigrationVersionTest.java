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
package com.contrastsecurity.cassandra.migration.info;

import com.contrastsecurity.cassandra.migration.CassandraMigrationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests MigrationVersion.
 */
public class MigrationVersionTest {
    @Test
    public void compareTo() {
        MigrationVersion v1 = MigrationVersion.fromVersion("1");
        MigrationVersion v10 = MigrationVersion.fromVersion("1.0");
        MigrationVersion v11 = MigrationVersion.fromVersion("1.1");
        MigrationVersion v1100 = MigrationVersion.fromVersion("1.1.0.0");
        MigrationVersion v1101 = MigrationVersion.fromVersion("1.1.0.1");
        MigrationVersion v2 = MigrationVersion.fromVersion("2");
        MigrationVersion v201004171859 = MigrationVersion.fromVersion("201004171859");
        MigrationVersion v201004180000 = MigrationVersion.fromVersion("201004180000");

        assertThat(v1.compareTo(v10)).isZero();
        assertThat(v10.compareTo(v1)).isZero();
        assertThat(v1.compareTo(v11)).isNegative();
        assertThat(v11.compareTo(v1)).isPositive();
        assertThat(v11.compareTo(v1100)).isZero();
        assertThat(v1100.compareTo(v11)).isZero();
        assertThat(v11.compareTo(v1101)).isNegative();
        assertThat(v1101.compareTo(v11)).isPositive();
        assertThat(v1101.compareTo(v2)).isNegative();
        assertThat(v2.compareTo(v1101)).isPositive();
        assertThat(v201004171859.compareTo(v201004180000)).isNegative();
        assertThat(v201004180000.compareTo(v201004171859)).isPositive();

        assertThat(v2.compareTo(MigrationVersion.LATEST)).isNegative();
        assertThat(MigrationVersion.LATEST.compareTo(v2)).isPositive();
        assertThat(v201004180000.compareTo(MigrationVersion.LATEST)).isNegative();
        assertThat(MigrationVersion.LATEST.compareTo(v201004180000)).isPositive();
    }

    @Test
    public void testEquals() {
        final MigrationVersion a1 = MigrationVersion.fromVersion("1.2.3.3");
        final MigrationVersion a2 = MigrationVersion.fromVersion("1.2.3.3");
        assertThat(a1.compareTo(a2)).isZero();
        assertThat(a2.hashCode()).isEqualTo(a1.hashCode());
    }

    @Test
    public void testNumber() {
        final MigrationVersion a1 = MigrationVersion.fromVersion("1.2.13.3");
        final MigrationVersion a2 = MigrationVersion.fromVersion("1.2.3.3");
        assertThat(a1.compareTo(a2)).isPositive();
    }

    @Test
    public void testLength1() {
        final MigrationVersion a1 = MigrationVersion.fromVersion("1.2.1.3");
        final MigrationVersion a2 = MigrationVersion.fromVersion("1.2.1");
        assertThat(a1.compareTo(a2)).isPositive();
    }

    @Test
    public void testLength2() {
        final MigrationVersion a1 = MigrationVersion.fromVersion("1.2.1");
        final MigrationVersion a2 = MigrationVersion.fromVersion("1.2.1.1");
        assertThat(a1.compareTo(a2)).isNegative();
    }

    @Test
    public void leadingZeroes() {
        final MigrationVersion v1 = MigrationVersion.fromVersion("1.0");
        final MigrationVersion v2 = MigrationVersion.fromVersion("001.0");
        assertThat(v1.compareTo(v2)).isZero();
        assertThat(v2).isEqualTo(v1);
        assertThat(v2.hashCode()).isEqualTo(v1.hashCode());
    }

    @Test
    public void trailingZeroes() {
        final MigrationVersion v1 = MigrationVersion.fromVersion("1.00");
        final MigrationVersion v2 = MigrationVersion.fromVersion("1");
        assertThat(v1.compareTo(v2)).isZero();
        assertThat(v2).isEqualTo(v1);
        assertThat(v2.hashCode()).isEqualTo(v1.hashCode());
    }

    @Test
    public void fromVersionFactory() {
        assertThat(MigrationVersion.fromVersion(MigrationVersion.LATEST.getVersion())).isEqualTo(MigrationVersion.LATEST);
        assertThat(MigrationVersion.fromVersion(MigrationVersion.EMPTY.getVersion())).isEqualTo(MigrationVersion.EMPTY);
        assertThat(MigrationVersion.fromVersion("1.2.3").getVersion()).isEqualTo("1.2.3");
    }

    @Test
    public void empty() {
        assertThat(MigrationVersion.EMPTY).isEqualTo(MigrationVersion.EMPTY);
        assertThat(MigrationVersion.EMPTY.compareTo(MigrationVersion.EMPTY)).isZero();
    }


    @Test
    public void latest() {
        assertThat(MigrationVersion.LATEST).isEqualTo(MigrationVersion.LATEST);
        assertThat(MigrationVersion.LATEST.compareTo(MigrationVersion.LATEST)).isZero();
    }

    @Test
    public void zeros() {
        final MigrationVersion v1 = MigrationVersion.fromVersion("0.0");
        final MigrationVersion v2 = MigrationVersion.fromVersion("0");
        assertThat(v1.compareTo(v2)).isZero();
        assertThat(v2).isEqualTo(v1);
        assertThat(v2.hashCode()).isEqualTo(v1.hashCode());
    }

    @Test
    public void missingNumber() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion("1..1"));
    }

    @Test
    public void dot() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion("."));
    }

    @Test
    public void startDot() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion(".1"));
    }

    @Test
    public void endDot() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion("1."));
    }

    @Test
    public void letters() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion("abc1.0"));
    }

    @Test
    public void dash() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion("1.2.1-3"));
    }

    @Test
    public void alphaNumeric() {
        assertThrows(CassandraMigrationException.class, () -> MigrationVersion.fromVersion("1.2.1a-3"));
    }

    @Test
    public void testWouldOverflowLong() {
        final String raw = "9999999999999999999999999999999999.8888888231231231231231298797298789132.22";
        MigrationVersion longVersions = MigrationVersion.fromVersion(raw);
        assertThat(longVersions.getVersion()).isEqualTo(raw);
    }
}

