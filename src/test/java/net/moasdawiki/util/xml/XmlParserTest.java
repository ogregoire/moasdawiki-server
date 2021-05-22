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

package net.moasdawiki.util.xml;

import java.util.List;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Testet den XML-Parser.
 */
public class XmlParserTest {
	private static final String XMLDECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private XmlParser xmlParser;

	@BeforeMethod
	public void beforeTest() {
		Logger logger = new Logger(null);
		xmlParser = new XmlParser(logger);
	}

	@Test
	public void testEmptyClass() throws Exception {
		String xml = XMLDECL + "<EmptyClass></EmptyClass>";
		EmptyClass result = xmlParser.parse(xml, EmptyClass.class);
		assertNotNull(result);
	}

	@XmlRootElement
	public static class EmptyClass {
	}

	@Test
	public void testXmlDeclarationMissing() throws Exception {
		String xml = "<EmptyClass></EmptyClass>";
		EmptyClass result = xmlParser.parse(xml, EmptyClass.class);
		assertNotNull(result);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testXmlDeclarationIncomplete() throws Exception {
		String xml = "<?xml";
		xmlParser.parse(xml, EmptyClass.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testRootElementAnnotationMissing() throws Exception {
		String xml = XMLDECL + "<EmptyClass2></EmptyClass2>";
		xmlParser.parse(xml, EmptyClass2.class);
	}

	public static class EmptyClass2 {
	}

	@Test
	public void testRootElementAnnotationName() throws Exception {
		String xml = XMLDECL + "<root></root>";
		EmptyClass3 result = xmlParser.parse(xml, EmptyClass3.class);
		assertNotNull(result);
	}

	@XmlRootElement(name = "root")
	public static class EmptyClass3 {
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testRootElementAnnotationNameWrong() throws Exception {
		String xml = XMLDECL + "<EmptyClass></EmptyClass>";
		xmlParser.parse(xml, EmptyClass3.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testRootElementIncomplete1() throws Exception {
		String xml = XMLDECL + "<";
		xmlParser.parse(xml, EmptyClass3.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testRootElementIncomplete2() throws Exception {
		String xml = XMLDECL + "<root";
		xmlParser.parse(xml, EmptyClass3.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testRootElementIncomplete3() throws Exception {
		String xml = XMLDECL + "<root/";
		xmlParser.parse(xml, EmptyClass3.class);
	}

	@Test
	public void testAttribute() throws Exception {
		String xml = XMLDECL + "<root attr=\"value &amp; space\"></root>";
		AttributeClass result = xmlParser.parse(xml, AttributeClass.class);
		assertEquals("value & space", result.attr);
	}

	@XmlRootElement(name = "root")
	public static class AttributeClass {
		@XmlAttribute
		public String attr;
	}

	/**
	 * Testet syntaktische Grenzfälle, d.h. ohne Anführungszeichen und nicht
	 * gemappte Attributnamen.
	 */
	@Test
	public void testAttribute2() throws Exception {
		String xml = XMLDECL + "<root attr=value1 attr2=\"value2 with space\" attr4=/>";
		AttributeClass2 result = xmlParser.parse(xml, AttributeClass2.class);
		assertEquals("value1", result.attr1);
		assertNull(result.attr2);
		assertNull(result.attr3);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testAttribute3() throws Exception {
		String xml = XMLDECL + "<root attr";
		xmlParser.parse(xml, AttributeClass2.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testAttribute4() throws Exception {
		String xml = XMLDECL + "<root attr=";
		xmlParser.parse(xml, AttributeClass2.class);
	}

	@XmlRootElement(name = "root")
	public static class AttributeClass2 {
		@XmlAttribute(name = "attr")
		public String attr1;
		// no annotation --> Log warning
		public String attr2;
		@XmlAttribute
		public String attr3;
	}

	@Test
	public void testElement() throws Exception {
		String xml = XMLDECL + "<root><elem>value &amp; space</elem></root>";
		ElementClass result = xmlParser.parse(xml, ElementClass.class);
		assertEquals("value & space", result.elem);
	}

	@XmlRootElement(name = "root")
	public static class ElementClass {
		@XmlElement
		public String elem;
	}

	@Test
	public void testElementName() throws Exception {
		String xml = XMLDECL + "<root><field>  value with space  </field></root>";
		ElementClass2 result = xmlParser.parse(xml, ElementClass2.class);
		assertEquals("value with space", result.elem);
	}

	@XmlRootElement(name = "root")
	public static class ElementClass2 {
		@XmlElement(name = "field")
		public String elem;
	}

	@Test
	public void testElements1() throws Exception {
		String xml = XMLDECL + "<root><elem1>value1</elem1><elem2>value2</elem2></root>";
		ElementClass3 result = xmlParser.parse(xml, ElementClass3.class);
		assertEquals("value1", result.elem1);
		assertNull(result.elem2);
		assertNull(result.elem3);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testElements2() throws Exception {
		String xml = XMLDECL + "<root><";
		xmlParser.parse(xml, ElementClass3.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testElements3() throws Exception {
		String xml = XMLDECL + "<root><elem1";
		xmlParser.parse(xml, ElementClass3.class);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testElements4() throws Exception {
		String xml = XMLDECL + "<root><elem1/";
		xmlParser.parse(xml, ElementClass3.class);
	}

	@Test
	public void testElements5() throws Exception {
		String xml = XMLDECL + "<root><elem1/></root>";
		ElementClass3 result = xmlParser.parse(xml, ElementClass3.class);
		assertNull(result.elem1);
	}

	@XmlRootElement(name = "root")
	public static class ElementClass3 {
		@XmlElement
		public String elem1;
		// no annotation
		public String elem2;
		@XmlElement
		public String elem3;
	}

	@Test
	public void testElementBoolean() throws Exception {
		String xml = XMLDECL + "<root><elem1>true</elem1><elem1>false</elem1></root>";
		ElementBooleanClass result = xmlParser.parse(xml, ElementBooleanClass.class);
		assertTrue(result.elem1);
	}

	@XmlRootElement(name = "root")
	public static class ElementBooleanClass {
		@XmlElement
		public Boolean elem1;
	}

	@Test
	public void testElementStringList() throws Exception {
		String xml = XMLDECL + "<root><elem>value1</elem><elem>value2</elem></root>";
		ElementStringListClass result = xmlParser.parse(xml, ElementStringListClass.class);
		assertEquals(2, result.elem.size());
		assertEquals("value1", result.elem.get(0));
		assertEquals("value2", result.elem.get(1));
	}

	@XmlRootElement(name = "root")
	public static class ElementStringListClass {
		@XmlElement
		public List<String> elem;
	}

	@Test
	public void testElementObject() throws Exception {
		String xml = XMLDECL + "<root><sub><elem2>value</elem2></sub></root>";
		ElementObjectClass1 result = xmlParser.parse(xml, ElementObjectClass1.class);
		assertEquals("value", result.elem1.elem2);
	}

	@XmlRootElement(name = "root")
	public static class ElementObjectClass1 {
		@XmlElementRef
		public ElementObjectSubClass1 elem1;
	}

	@XmlRootElement(name = "sub")
	public static class ElementObjectSubClass1 {
		@XmlElement
		public String elem2;
	}

	@Test
	public void testElementObjectList() throws Exception {
		String xml = XMLDECL + "<root><sub><elem2>value1</elem2></sub><sub><elem2>value2</elem2></sub></root>";
		ElementObjectClass2 result = xmlParser.parse(xml, ElementObjectClass2.class);
		assertEquals(2, result.elem1.size());
		assertEquals("value1", result.elem1.get(0).elem2);
		assertEquals("value2", result.elem1.get(1).elem2);
	}

	@XmlRootElement(name = "root")
	public static class ElementObjectClass2 {
		@XmlElementRef
		public List<ElementObjectSubClass2> elem1;
	}

	@XmlRootElement(name = "sub")
	public static class ElementObjectSubClass2 {
		@XmlElement
		public String elem2;
	}

	/**
	 * Wert zwischen Tags einlesen, white-space im Text muss erhalten bleiben,
	 * außerhalb wird er weggeschnitten.
	 */
	@Test
	public void testRootValue1() throws Exception {
		String xml = XMLDECL + "<root>  value with space  </root>";
		ValueClass1 result = xmlParser.parse(xml, ValueClass1.class);
		assertEquals("value with space", result.elem1);
	}

	@XmlRootElement(name = "root")
	public static class ValueClass1 {
		@XmlValue
		public String elem1;
	}

	/**
	 * Slash-Zeichen außerhalb eines Tags testen.
	 */
	@Test
	public void testRootValue2() throws Exception {
		String xml = XMLDECL + "<root>/path/value</root>";
		ValueClass1 result = xmlParser.parse(xml, ValueClass1.class);
		assertEquals("/path/value", result.elem1);
	}

	@Test
	public void testRootValue3() throws Exception {
		String xml = XMLDECL + "<root> value1 &amp; space <elem2> value2 with space </elem2></root>";
		ValueClass3 result = xmlParser.parse(xml, ValueClass3.class);
		assertEquals("value1 & space", result.elem1);
		assertEquals("value2 with space", result.elem2);
	}

	@XmlRootElement(name = "root")
	public static class ValueClass3 {
		@XmlValue
		public String elem1;
		@XmlElement
		public String elem2;
	}

	@Test
	public void testUnescapeXmlValue() {
		assertEquals(xmlParser.unescapeXmlValue("a&lt;&gt;&amp;&quot;&apos;b"), "a<>&\"'b");
		assertEquals(xmlParser.unescapeXmlValue("a"), "a");
	}

	@Test
	public void testUnescapeXmlChar() {
		assertEquals(xmlParser.unescapeXmlChar("&lt;"), "<");
		assertEquals(xmlParser.unescapeXmlChar("&gt;"), ">");
		assertEquals(xmlParser.unescapeXmlChar("&amp;"), "&");
		assertEquals(xmlParser.unescapeXmlChar("&quot;"), "\"");
		assertEquals(xmlParser.unescapeXmlChar("&apos;"), "'");
		assertEquals(xmlParser.unescapeXmlChar("a"), "a");
	}
}
