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

package net.moasdawiki.util;

import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * Testet Hilfsmethoden der Klasse EscapeUtils.
 *
 * @author Herbert Reiter
 */
@SuppressWarnings({"CharsetObjectCanBeUsed", "ConstantConditions"})
public class EscapeUtilsTest {

	@Test
	public void testEscapeHtmlValid() {
		String original = "Text with \", ', lower <, greater >, and A&B";
		String escaped = EscapeUtils.escapeHtml(original);
		assertEquals(escaped, "Text with &quot;, &apos;, lower &lt;, greater &gt;, and A&amp;B");
	}

	@Test
	public void testEscapeHtmlNull() {
		assertNull(EscapeUtils.escapeHtml(null));
	}

	@Test
	public void testEncodeUrlNoEscape() {
		String result = EscapeUtils.encodeUrl("moasdawiki.net/path-1/sub_path!/an*other~seg%ment/(')/?param=value#anchor");
		assertEquals(result, "moasdawiki.net/path-1/sub_path!/an*other~seg%ment/(')/?param=value#anchor");
	}

	@Test
	public void testEncodeUrlEscape() {
		String result = EscapeUtils.encodeUrl("/a b/äöüß§$");
		assertEquals(result, "/a+b/%c3%a4%c3%b6%c3%bc%c3%9f%c2%a7%24");
	}

	@Test
	public void testEncodeUrlNull() {
		assertNull(EscapeUtils.encodeUrl(null));
	}

	@Test
	public void testEncodeUrlParameterNormal() {
		String result = EscapeUtils.encodeUrlParameter("path?param=value&another");
		assertEquals(result, "path%3Fparam%3Dvalue%26another");
	}

	@Test
	public void testEncodeUrlParameterNull() {
		assertNull(EscapeUtils.encodeUrlParameter(null));
	}

	@Test
	public void testPagePath2UrlNormal() {
		String result = EscapeUtils.pagePath2Url("Page!name#with?special%characters");
		assertEquals(result, "Page!21name!23with!3fspecial!25characters");
	}

	@Test
	public void testPagePath2UrlNull() {
		assertNull(EscapeUtils.pagePath2Url(null));
	}

	@Test
	public void testUrl2PagePathNormal() {
		String result = EscapeUtils.url2PagePath("Page!21name!23with!3fspecial!25characters");
		assertEquals(result, "Page!name#with?special%characters");
	}

	@Test
	public void testUrl2PagePathNull() {
		assertNull(EscapeUtils.url2PagePath(null));
	}

	@Test
	public void testChar2Hex() {
		assertEquals(EscapeUtils.char2Hex('§'), "a7");
		assertEquals(EscapeUtils.char2Hex('a'), "61");
		assertEquals(EscapeUtils.char2Hex('\t'), "09");
	}

	@Test
	public void testEncodeBase64() throws Exception {
		assertEquals("", EscapeUtils.encodeBase64("".getBytes()));
		assertEquals("QUJD", EscapeUtils.encodeBase64("ABC".getBytes()));
		assertEquals("QUJDRA==", EscapeUtils.encodeBase64("ABCD".getBytes()));
		assertEquals("QUJDREU=", EscapeUtils.encodeBase64("ABCDE".getBytes()));
		assertEquals("w6TDtsO8w58=", EscapeUtils.encodeBase64("äöüß".getBytes("UTF-8")));
	}

	@Test
	public void testDecodeBase64() throws Exception {
		assertTrue(Arrays.equals("".getBytes(), EscapeUtils.decodeBase64("")));
		assertTrue(Arrays.equals("ABC".getBytes(), EscapeUtils.decodeBase64("QUJD")));
		assertTrue(Arrays.equals("ABCD".getBytes(), EscapeUtils.decodeBase64("QUJDRA==")));
		assertTrue(Arrays.equals("ABCDE".getBytes(), EscapeUtils.decodeBase64("QUJDREU=")));
		assertTrue(Arrays.equals("äöüß".getBytes("UTF-8"), EscapeUtils.decodeBase64("w6TDtsO8w58=")));
	}
}
