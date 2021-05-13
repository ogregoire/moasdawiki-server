/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki;

import java.util.Collection;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Assert helper methods for better error messages.
 */
public abstract class AssertHelper {

    public static void assertIsEmpty(Collection<?> collection) {
        assertTrue(collection.isEmpty(), "collection is not empty: " + collection);
    }

    public static void assertIsEmpty(Map<?, ?> map) {
        assertTrue(map.isEmpty(), "map is not empty: " + map);
    }

    public static <T> void assertContains(Collection<T> collection, T element) {
        assertTrue(collection.contains(element), "element '" + element + "' expected in collection " + collection);
    }

    public static void assertContains(String text, String snippetToContain) {
        assertTrue(text.contains(snippetToContain),
                "text '" + text + "' doesn't contain '" + snippetToContain + "'");
    }

    public static <T> void assertContainsNot(Collection<T> collection, T element) {
        assertFalse(collection.contains(element), "element '" + element + "' not expected in collection " + collection);
    }

    public static <K> void assertContainsKey(Map<K, ?> map, K key) {
        assertTrue(map.containsKey(key), "key '" + key + "' expected in map " + map);
    }

    public static <K> void assertContainsKeyNot(Map<K, ?> map, K key) {
        assertFalse(map.containsKey(key), "key '" + key + "' not expected in map " + map);
    }

    public static void assertEndsWith(String text, String snippetToEnd) {
        assertTrue(text.endsWith(snippetToEnd),
                "text '" + text + "' doesn't end with '" + snippetToEnd + "'");
    }
}
