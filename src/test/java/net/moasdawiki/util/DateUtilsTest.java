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

package net.moasdawiki.util;

import net.moasdawiki.base.ServiceException;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.testng.Assert.*;

@SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
public class DateUtilsTest {

    @Test
    public void testParseUtcDateValid() throws Exception {
        Date date = DateUtils.parseUtcDate("2019-05-20T14:30:45.123Z");
        assertNotNull(date);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        assertEquals(cal.get(Calendar.YEAR), 2019);
        assertEquals(cal.get(Calendar.MONTH), 4); // 0 = January
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 20);
        assertEquals(cal.get(Calendar.HOUR_OF_DAY), 14);
        assertEquals(cal.get(Calendar.MINUTE), 30);
        assertEquals(cal.get(Calendar.SECOND), 45);
        assertEquals(cal.get(Calendar.MILLISECOND), 123);
    }

    @Test
    public void testParseUtcDateNull() throws Exception {
        Date date = DateUtils.parseUtcDate(null);
        assertNull(date);
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testParseUtcDateEmpty() throws Exception {
        DateUtils.parseUtcDate("");
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testParseUtcDateError() throws Exception {
        DateUtils.parseUtcDate("abcde");
    }

    @Test
    public void testFormatUtcDateValid() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        //noinspection MagicConstant
        cal.set(2019, 4, 20,14, 30,45);
        cal.set(Calendar.MILLISECOND, 123);
        String dateStr = DateUtils.formatUtcDate(cal.getTime());
        assertEquals(dateStr, "2019-05-20T14:30:45.123Z");
    }

    @Test
    public void testFormatUtcDateNull() {
        String dateStr = DateUtils.formatUtcDate(null);
        assertNull(dateStr);
    }

    @Test
    public void testFormatDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        //noinspection MagicConstant
        cal.set(2019, 4, 20,14, 30,45);
        String dateStr = DateUtils.formatDate(cal.getTime(), "yyyy.MM.dd");
        assertEquals(dateStr, "2019.05.20");
        // no need to test more formats as this would test SimpleDateFormat
    }
}
