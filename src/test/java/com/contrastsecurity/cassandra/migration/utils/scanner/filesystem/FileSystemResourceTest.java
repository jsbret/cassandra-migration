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
package com.contrastsecurity.cassandra.migration.utils.scanner.filesystem;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSystemResourceTest {
    @Test
    public void getFilename() throws Exception {
        assertThat(new FileSystemResource("Mig777__Test.cql").getFilename()).isEqualTo("Mig777__Test.cql");
        assertThat(new FileSystemResource("folder/Mig777__Test.cql").getFilename()).isEqualTo("Mig777__Test.cql");
    }

    @Test
    public void getPath() throws Exception {
        assertThat(new FileSystemResource("Mig777__Test.cql").getLocation()).isEqualTo("Mig777__Test.cql");
        assertThat(new FileSystemResource("folder/Mig777__Test.cql").getLocation()).isEqualTo("folder/Mig777__Test.cql");
    }
}
