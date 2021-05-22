/*
 * MoasdaWiki Server
 *
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation (AGPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.moasdawiki.service.wiki.parser;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.StringReader;

import static org.testng.Assert.*;

public class LineReaderTest {

    private LineReader lineReader;

    @BeforeMethod
    public void setUp() throws Exception {
        String text = "line1\n"
                + "line2\r\n"
                + "line3\r"
                + "line4\n";
        lineReader = new LineReader(new StringReader(text));
    }

    @Test
    public void testNextLine() throws Exception {
        lineReader.nextLine();
        assertEquals(lineReader.getLine(), "line2");
    }

    @Test
    public void testGetLine() throws Exception {
        assertEquals(lineReader.getLine(), "line1");
        assertEquals(lineReader.getLine(), "line1");
        lineReader.nextLine();
        assertEquals(lineReader.getLine(), "line2");
        lineReader.nextLine();
        assertEquals(lineReader.getLine(), "line3");
        lineReader.nextLine();
        assertEquals(lineReader.getLine(), "line4");
        lineReader.nextLine();
        assertNull(lineReader.getLine()); // eof
    }

    @Test
    public void testGetCharsReadTotal() throws Exception {
        assertEquals(lineReader.getCharsReadTotal(), 0);
        lineReader.nextLine();
        assertEquals(lineReader.getCharsReadTotal(), 6);
        lineReader.nextLine();
        assertEquals(lineReader.getCharsReadTotal(), 13);
        lineReader.nextLine();
        assertEquals(lineReader.getCharsReadTotal(), 19);
        lineReader.nextLine();
        assertEquals(lineReader.getCharsReadTotal(), 25);
        lineReader.nextLine(); // eof
        assertEquals(lineReader.getCharsReadTotal(), 25);
    }

    @Test
    public void testGetCharsReadLine() throws Exception {
        assertEquals(lineReader.getCharsReadLine(), 0);
        lineReader.nextLine();
        assertEquals(lineReader.getCharsReadLine(), 0);
    }

    @Test
    public void testSetCharsReadLine() throws Exception {
        // first line
        assertEquals(lineReader.getCharsReadLine(), 0);
        assertEquals(lineReader.getCharsReadTotal(), 0);
        lineReader.setCharsReadLine(5);
        assertEquals(lineReader.getCharsReadLine(), 5);
        assertEquals(lineReader.getCharsReadTotal(), 5);
        lineReader.setCharsReadLine(3);
        assertEquals(lineReader.getCharsReadLine(), 3);
        assertEquals(lineReader.getCharsReadTotal(), 3);

        // second line
        lineReader.nextLine();
        assertEquals(lineReader.getCharsReadLine(), 0);
        assertEquals(lineReader.getCharsReadTotal(), 6);
        lineReader.setCharsReadLine(5);
        assertEquals(lineReader.getCharsReadLine(), 5);
        assertEquals(lineReader.getCharsReadTotal(), 11);
        lineReader.setCharsReadLine(0);
        assertEquals(lineReader.getCharsReadLine(), 0);
        assertEquals(lineReader.getCharsReadTotal(), 6);
    }

    @Test
    public void testEof() throws Exception {
        assertFalse(lineReader.eof());
        lineReader.nextLine();
        assertFalse(lineReader.eof());
        lineReader.nextLine();
        assertFalse(lineReader.eof());
        lineReader.nextLine();
        assertFalse(lineReader.eof());
        lineReader.nextLine();
        assertTrue(lineReader.eof());
    }
}
