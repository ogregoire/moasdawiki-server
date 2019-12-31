package net.moasdawiki.util;

import net.moasdawiki.base.ServiceException;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

@SuppressWarnings("ConstantConditions")
public class StringUtilsTest {

    @Test
    public void testIsNumeric() {
        assertTrue(StringUtils.isNumeric("12345"));
        assertFalse(StringUtils.isNumeric("-12345"));
        assertFalse(StringUtils.isNumeric("123k45"));
        assertFalse(StringUtils.isNumeric(" 123 "));
        assertFalse(StringUtils.isNumeric(""));
        assertFalse(StringUtils.isNumeric(null));
    }

    @Test
    public void testParseInteger() throws Exception {
        assertEquals(StringUtils.parseInteger("12345"), Integer.valueOf(12345));
        assertEquals(StringUtils.parseInteger("-12345"), Integer.valueOf(-12345));
        assertNull(StringUtils.parseInteger(""));
        assertNull(StringUtils.parseInteger(null));
    }

    @Test(expectedExceptions = ServiceException.class)
    public void testParseIntegerError() throws Exception {
        StringUtils.parseInteger("abc");
    }

    @Test
    public void testConcatList() {
        assertEquals(StringUtils.concat(Arrays.asList("a", "b", "c"), "<>"), "a<>b<>c");
        assertEquals(StringUtils.concat(Arrays.asList("a", "b", "c"), ""), "abc");
        assertEquals(StringUtils.concat(Arrays.asList("a", "b", "c"), null), "abc");
        assertEquals(StringUtils.concat(Collections.singletonList("a"), "<>"), "a");
        assertEquals(StringUtils.concat(Collections.emptyList(), "<>"), "");
        assertEquals(StringUtils.concat((List<String>) null, "<>"), "");
    }

    @Test
    public void testConcatArray() {
        assertEquals(StringUtils.concat(new String[]{ "a", "b", "c" }, "<>"), "a<>b<>c");
        assertEquals(StringUtils.concat(new String[]{ "a", "b", "c" }, ""), "abc");
        assertEquals(StringUtils.concat(new String[]{ "a", "b", "c" }, null), "abc");
        assertEquals(StringUtils.concat(new String[]{ "a" }, "<>"), "a");
        assertEquals(StringUtils.concat(new String[0], "<>"), "");
        assertEquals(StringUtils.concat((String[]) null, "<>"), "");
    }

    @Test
    public void testSplitByWhitespace() {
        assertEquals(StringUtils.splitByWhitespace("a b c"), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace(" a b c"), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace("a b c "), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace("a  b  c"), new String[]{ "a", "b", "c" });
        assertEquals(StringUtils.splitByWhitespace("a \t\r\nb"), new String[]{ "a", "b" });
        assertEquals(StringUtils.splitByWhitespace("a"), new String[]{ "a" });
        assertEquals(StringUtils.splitByWhitespace(""), new String[0]);
        assertEquals(StringUtils.splitByWhitespace(null), new String[0]);
    }

    @Test
    public void testNullToEmpty() {
        assertEquals(StringUtils.nullToEmpty("abc"), "abc");
        assertEquals(StringUtils.nullToEmpty(""), "");
        assertEquals(StringUtils.nullToEmpty(null), "");
    }

    @Test
    public void testEmptyToNull() {
        assertEquals(StringUtils.emptyToNull("abc"), "abc");
        assertNull(StringUtils.emptyToNull(""));
        assertNull(StringUtils.emptyToNull(null));
    }
}
