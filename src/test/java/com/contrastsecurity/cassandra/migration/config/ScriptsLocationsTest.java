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
package com.contrastsecurity.cassandra.migration.config;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Small Test for Locations.
 */
public class ScriptsLocationsTest {
    @Test
    public void mergeLocations() {
        ScriptsLocations locations = new ScriptsLocations("db/locations", "db/files", "db/classes");
        List<ScriptsLocation> locationList = locations.getLocations();
        assertThat(locationList).hasSize(3);
        Iterator<ScriptsLocation> iterator = locationList.iterator();
        assertThat(iterator.next().getPath()).isEqualTo("db/classes");
        assertThat(iterator.next().getPath()).isEqualTo("db/files");
        assertThat(iterator.next().getPath()).isEqualTo("db/locations");
    }

    @Test
    public void mergeLocationsDuplicate() {
        ScriptsLocations locations = new ScriptsLocations("db/locations", "db/migration", "db/migration");
        List<ScriptsLocation> locationList = locations.getLocations();
        assertThat(locationList).hasSize(2);
        Iterator<ScriptsLocation> iterator = locationList.iterator();
        assertThat(iterator.next().getPath()).isEqualTo("db/locations");
        assertThat(iterator.next().getPath()).isEqualTo("db/migration");
    }

    @Test
    public void mergeLocationsOverlap() {
        ScriptsLocations locations = new ScriptsLocations("db/migration/oracle", "db/migration", "db/migration");
        List<ScriptsLocation> locationList = locations.getLocations();
        assertThat(locationList).hasSize(1);
        assertThat(locationList.get(0).getPath()).isEqualTo("db/migration");
    }

    @Test
    public void mergeLocationsSimilarButNoOverlap() {
        ScriptsLocations locations = new ScriptsLocations("db/migration/oracle", "db/migration", "db/migrationtest");
        List<ScriptsLocation> locationList = locations.getLocations();
        assertThat(locationList).hasSize(2);
        assertThat(locationList).contains(new ScriptsLocation("db/migration"));
        assertThat(locationList).contains(new ScriptsLocation("db/migrationtest"));
    }

    @Test
    public void mergeLocationsSimilarButNoOverlapCamelCase() {
        ScriptsLocations locations = new ScriptsLocations("/com/xxx/Star/", "/com/xxx/StarTrack/");
        List<ScriptsLocation> locationList = locations.getLocations();
        assertThat(locationList).hasSize(2);
        assertThat(locationList).contains(new ScriptsLocation("com/xxx/Star"));
        assertThat(locationList).contains(new ScriptsLocation("com/xxx/StarTrack"));
    }

    @Test
    public void mergeLocationsSimilarButNoOverlapHyphen() {
        ScriptsLocations locations = new ScriptsLocations("db/migration/oracle", "db/migration", "db/migration-test");
        List<ScriptsLocation> locationList = locations.getLocations();
        assertThat(locationList).hasSize(2);
        assertThat(locationList).contains(new ScriptsLocation("db/migration"));
        assertThat(locationList).contains(new ScriptsLocation("db/migration-test"));
    }
}
