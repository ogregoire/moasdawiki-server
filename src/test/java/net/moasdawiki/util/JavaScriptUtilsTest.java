package net.moasdawiki.util;

import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class JavaScriptUtilsTest {

    @Test
    public void testToArray() {
        String result = JavaScriptUtils.toArray(Arrays.asList("a", "b", "c"));
        assertEquals(result, "[\"a\", \"b\", \"c\"]");
    }

    @Test
    public void testEscapeJavaScriptNormal() {
        String result = JavaScriptUtils.escapeJavaScript("abc\"'\\\b\f\n\r\tdef");
        assertEquals(result, "abc\\\"\\'\\\\\\b\\f\\n\\r\\tdef");
    }

    @Test
    public void testEscapeJavaScriptNull() {
        assertNull(JavaScriptUtils.escapeJavaScript(null));
    }
}
