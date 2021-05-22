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

package net.moasdawiki.service.render;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class JavaFormatterTest {

    @Test
    public void testFormatKeywords() {
        for (String keyword : JavaFormatter.KEYWORDS) {
            String formatted = new JavaFormatter(keyword).format();
            assertEquals(formatted, "<span class=\"code-java-keyword\">" + keyword + "</span>");
        }
    }

    @Test
    public void testFormatNonJavaText() {
        String formatted = new JavaFormatter("non-java-text").format();
        assertEquals(formatted, "non-java-text");
    }

    @Test
    public void testFormatLineBreak() {
        String formatted = new JavaFormatter("line1\nline2").format();
        assertEquals(formatted, "line1<br>\nline2");
    }

    @Test
    public void testFormatSingleLineComment() {
        {
            String formatted = new JavaFormatter("// single line comment").format();
            assertEquals(formatted, "<span class=\"code-java-comment\">//&nbsp;single&nbsp;line&nbsp;comment</span>");
        }
        {
            String formatted = new JavaFormatter("// with linebreak\n").format();
            assertEquals(formatted, "<span class=\"code-java-comment\">//&nbsp;with&nbsp;linebreak</span><br>\n");
        }
    }

    @Test
    public void testFormatMultipleLineComment() {
        {
            String codeText = "/* multi\n"
                    + "line\n"
                    + "comment */";
            String formatted = new JavaFormatter(codeText).format();
            assertEquals(formatted, "<span class=\"code-java-comment\">/*&nbsp;multi</span><br>\n"
                    + "<span class=\"code-java-comment\">line</span><br>\n"
                    + "<span class=\"code-java-comment\">comment&nbsp;*/</span>");
        }
        {
            String codeText = "/* one line\n"
                    + "*/";
            String formatted = new JavaFormatter(codeText).format();
            assertEquals(formatted, "<span class=\"code-java-comment\">/*&nbsp;one&nbsp;line</span><br>\n"
                    + "<span class=\"code-java-comment\">*/</span>");
        }
    }

    @Test
    public void testFormatString() {
        {
            String formatted = new JavaFormatter("\"text \\t content\"").format();
            assertEquals(formatted, "<span class=\"code-java-string\">&quot;text&nbsp;\\t&nbsp;content&quot;</span>");
        }
        {
            String formatted = new JavaFormatter("\"without end").format();
            assertEquals(formatted, "<span class=\"code-java-string\">&quot;without&nbsp;end</span>");
        }
        {
            String formatted = new JavaFormatter("\"without end\n").format();
            assertEquals(formatted, "<span class=\"code-java-string\">&quot;without&nbsp;end</span><br>\n");
        }
    }

    @Test
    public void testFormatChar() {
        String formatted = new JavaFormatter("'c'").format();
        assertEquals(formatted, "<span class=\"code-java-string\">&apos;c&apos;</span>");
    }
}
