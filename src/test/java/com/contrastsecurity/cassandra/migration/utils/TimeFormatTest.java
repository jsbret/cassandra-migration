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
package com.contrastsecurity.cassandra.migration.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Small test for TimeFormat
 */
public class TimeFormatTest {
    @Test
    public void format() {
        assertThat(TimeFormat.format(1)).isEqualTo("00:00.001s");
        assertThat(TimeFormat.format(12)).isEqualTo("00:00.012s");
        assertThat(TimeFormat.format(123)).isEqualTo("00:00.123s");
        assertThat(TimeFormat.format(1234)).isEqualTo("00:01.234s");
        assertThat(TimeFormat.format(12345)).isEqualTo("00:12.345s");
        assertThat(TimeFormat.format(60000 + 23456)).isEqualTo("01:23.456s");
        assertThat(TimeFormat.format((60000 * 12) + 34567)).isEqualTo("12:34.567s");
        assertThat(TimeFormat.format((60000 * 123) + 45678)).isEqualTo("123:45.678s");
    }
}
