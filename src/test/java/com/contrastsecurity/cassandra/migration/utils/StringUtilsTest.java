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
 * Testcase for StringUtils.
 */
public class StringUtilsTest {
    @Test
    public void trimOrPad() {
        assertThat(StringUtils.trimOrPad("Hello World", 15)).isEqualTo("Hello World    ");
        assertThat(StringUtils.trimOrPad("Hello World", 10)).isEqualTo("Hello Worl");
        assertThat(StringUtils.trimOrPad(null, 10)).isEqualTo("          ");
    }

    @Test
    public void isNumeric() {
        assertThat(StringUtils.isNumeric(null)).isFalse();
        assertThat(StringUtils.isNumeric("")).isTrue();
        assertThat(StringUtils.isNumeric("  ")).isFalse();
        assertThat(StringUtils.isNumeric("123")).isTrue();
        assertThat(StringUtils.isNumeric("12 3")).isFalse();
        assertThat(StringUtils.isNumeric("ab2c")).isFalse();
        assertThat(StringUtils.isNumeric("12-3")).isFalse();
        assertThat(StringUtils.isNumeric("12.3")).isFalse();
    }

    @Test
    public void collapseWhitespace() {
        assertThat(StringUtils.collapseWhitespace("")).isEqualTo("");
        assertThat(StringUtils.collapseWhitespace("abc")).isEqualTo("abc");
        assertThat(StringUtils.collapseWhitespace("a b")).isEqualTo("a b");
        assertThat(StringUtils.collapseWhitespace(" a ")).isEqualTo(" a ");
        assertThat(StringUtils.collapseWhitespace("  a  ")).isEqualTo(" a ");
        assertThat(StringUtils.collapseWhitespace("a          b")).isEqualTo("a b");
        assertThat(StringUtils.collapseWhitespace("a  b   c")).isEqualTo("a b c");
        assertThat(StringUtils.collapseWhitespace("   a b   c  ")).isEqualTo(" a b c ");
    }

    @Test
    public void tokenizeToStringArray() {
        assertThat(StringUtils.tokenizeToStringArray("abc", ",")).containsExactly("abc");
        assertThat(StringUtils.tokenizeToStringArray("abc,def", ",")).containsExactly("abc", "def");
        assertThat(StringUtils.tokenizeToStringArray(" abc ,def ", ",")).containsExactly("abc", "def");
        assertThat(StringUtils.tokenizeToStringArray(",abc", ",")).containsExactly("", "abc");
        assertThat(StringUtils.tokenizeToStringArray(" , abc", ",")).containsExactly("", "abc");
    }
}
