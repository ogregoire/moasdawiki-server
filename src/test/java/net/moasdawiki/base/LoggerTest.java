/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2020 Herbert Reiter (herbert@moasdawiki.net)
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

import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class LoggerTest {

    @Test
    public void testWriteMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Logger logger = new Logger(new PrintStream(out));
        logger.write("message");

        assertTrue(out.toString().endsWith("message\n"));
        assertEquals(out.size(), 36);
    }

    @Test
    public void testWriteMessageNoOut() {
        Logger logger = new Logger(null);
        logger.write("message");
    }

    @Test
    public void testWriteException() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Logger logger = new Logger(new PrintStream(out));
        logger.write("message", new Exception("errormessage"));

        assertTrue(out.toString().contains("message: (java.lang.Exception) errormessage\n"));
    }

    @Test
    public void testWriteExceptionNoOut() {
        Logger logger = new Logger(null);
        logger.write("message", new Exception());
    }
}
