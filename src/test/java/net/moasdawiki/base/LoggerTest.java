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

package net.moasdawiki.base;

import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static net.moasdawiki.AssertHelper.assertContains;
import static net.moasdawiki.AssertHelper.assertEndsWith;
import static org.testng.Assert.assertEquals;

public class LoggerTest {

    @Test
    public void testWriteMessage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Logger logger = new Logger(new PrintStream(out));
        logger.write("message");

        assertEndsWith(out.toString(), "message\n");
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

        assertContains(out.toString(), "message: (java.lang.Exception) errormessage\n");
    }

    @Test
    public void testWriteExceptionNoOut() {
        Logger logger = new Logger(null);
        logger.write("message", new Exception());
    }
}
