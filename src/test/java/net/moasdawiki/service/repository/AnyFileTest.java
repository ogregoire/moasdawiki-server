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

package net.moasdawiki.service.repository;

import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.*;

public class AnyFileTest {

    @SuppressWarnings({"SimplifiedTestNGAssertion", "ConstantConditions", "EqualsBetweenInconvertibleTypes"})
    @Test
    public void testEquals() {
        assertFalse(new AnyFile("filePath").equals(null));
        assertFalse(new AnyFile("filePath").equals("wrongtype"));
        assertTrue(new AnyFile("filePath").equals(new AnyFile("filePath")));
        assertTrue(new AnyFile("filePath").equals(new AnyFile("filePath", new Date())));
        assertFalse(new AnyFile("filePath1").equals(new AnyFile("filePath2")));
    }
}
