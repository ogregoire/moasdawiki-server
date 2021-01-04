/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.moasdawiki.base;

import net.moasdawiki.service.repository.RepositoryService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SettingsTest {

    private static final String CONFIG_FILE_CONTENT =
            "# comment\n" +
            "keyWithoutValue\n" +
            "stringKey=stringValue\n" +
            "stringListKey=value1\n" +
            "stringListKey=value2\n" +
            "stringListKey=value3\n" +
            "intKey=3\n" +
            "trueKey=true\n" +
            "falseKey=false\n";

    private Settings settings;

    @BeforeMethod
    public void setUp() throws Exception {
        RepositoryService repositoryService = mock(RepositoryService.class);
        when(repositoryService.readTextFile(any())).thenReturn(CONFIG_FILE_CONTENT);
        settings = new Settings(new Logger(null), repositoryService, "config.txt");
    }

    @Test
    public void testGetString() {
        assertEquals(settings.getString("stringKey"), "stringValue");
    }

    @Test
    public void testGetStringFromList() {
        assertEquals(settings.getString("stringListKey"), "value1");
    }

    @Test
    public void testGetStringDefault() {
        assertEquals(settings.getString("unknownKey", "defaultValue"), "defaultValue");
    }

    @Test
    public void testGetStringArray() {
        assertEquals(settings.getStringArray("stringKey"), new String[]{ "stringValue" });
        assertEquals(settings.getStringArray("stringListKey"), new String[]{ "value1", "value2", "value3" });
        assertEquals(settings.getStringArray("unknownKey", "value1"), new String[]{ "value1" });
    }

    @Test
    public void testGetInt() {
        assertEquals(settings.getInt("intKey", 5), 3);
        assertEquals(settings.getInt("unknownKey", 5), 5);
        assertEquals(settings.getInt("stringKey", 5), 5);
    }

    @Test
    public void testGetInteger() {
        assertEquals(settings.getInteger("intKey", 5), Integer.valueOf(3));
        assertEquals(settings.getInteger("unknownKey", 5), Integer.valueOf(5));
        assertNull(settings.getInteger("unknownKey", null));
    }

    @Test
    public void testGetBoolean() {
        assertTrue(settings.getBoolean("trueKey", false));
        assertTrue(settings.getBoolean("trueKey", true));

        assertFalse(settings.getBoolean("falseKey", false));
        assertFalse(settings.getBoolean("falseKey", true));

        assertTrue(settings.getBoolean("stringKey", true));
        assertFalse(settings.getBoolean("stringKey", false));

        assertTrue(settings.getBoolean("unknownKey", true));
        assertFalse(settings.getBoolean("unknownKey", false));
    }

    @Test
    public void testGetProgramName() {
        assertNotNull(settings.getProgramName());
    }

    @Test
    public void testGetVersion() {
        assertNotNull(settings.getVersion());
    }

    @Test
    public void testGetProgramNameVersion() {
        assertNotNull(settings.getProgramNameVersion());
    }
}
