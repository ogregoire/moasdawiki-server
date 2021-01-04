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

package net.moasdawiki.util.xml;

import java.util.ArrayList;
import java.util.List;

import net.moasdawiki.base.ServiceException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Testet den XML-Generator.
 */
public class XmlGeneratorTest {
	private static final String XMLDECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private XmlGenerator xmlGenerator;

	@BeforeMethod
	public void beforeTest() {
		xmlGenerator = new XmlGenerator(null);
	}

	@Test
	public void testEmptyClass() throws Exception {
		Object bean = new EmptyClass();
		String xml = xmlGenerator.generate(bean);
		assertEquals(XMLDECL + "<EmptyClass></EmptyClass>", xml);
	}

	@XmlRootElement
	private static class EmptyClass {
	}

	@Test
	public void testRootElementName() throws Exception {
		Object bean = new EmtpyClass2();
		String xml = xmlGenerator.generate(bean);
		assertEquals(XMLDECL + "<root></root>", xml);
	}

	@XmlRootElement(name = "root")
	private static class EmtpyClass2 {
	}

	@Test
	public void testAttributeAndElement() throws Exception {
		NormalClass bean = new NormalClass();
		bean.field1 = "value1 & space";
		bean.field2 = "value2 & space";
		bean.field3 = "value3 & space";
		bean.field4 = "value4 & space";
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<root field1=\"value1 &amp; space\">value4 &amp; space<field2>value2 &amp; space</field2></root>";
		assertEquals(exp, xml);
	}

	@Test
	public void testAttributeAndElementNull() throws Exception {
		NormalClass bean = new NormalClass();
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<root></root>";
		assertEquals(exp, xml);
	}

	@XmlRootElement(name = "root")
	private static class NormalClass {
		@XmlAttribute
		public String field1;
		@XmlElement
		public String field2;
		// no annotation
		public String field3;
		@XmlValue
		public String field4;
	}

	@Test
	public void testAttributeAndElementName() throws Exception {
		NormalClass2 bean = new NormalClass2();
		bean.field1 = "value1";
		bean.field2 = "value2";
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<root attr=\"value1\"><elem>value2</elem></root>";
		assertEquals(exp, xml);
	}

	@XmlRootElement(name = "root")
	private static class NormalClass2 {
		@XmlAttribute(name = "attr")
		public String field1;
		@XmlElement(name = "elem")
		public String field2;
	}

	@Test
	public void testObject() throws Exception {
		ObjectClass1 bean = new ObjectClass1();
		bean.field1 = new ObjectClass2();
		bean.field1.field2 = "value2";
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<object1><elem1><object2><elem2>value2</elem2></object2></elem1></object1>";
		assertEquals(exp, xml);
	}

	@XmlRootElement(name = "object1")
	private static class ObjectClass1 {
		@XmlElement(name = "elem1")
		public ObjectClass2 field1;
	}
	@XmlRootElement(name = "object2")
	private static class ObjectClass2 {
		@XmlElement(name = "elem2")
		public String field2;
	}

	@Test
	public void testObjectRef() throws Exception {
		ObjectRefClass1 bean = new ObjectRefClass1();
		bean.field1 = new ObjectRefClass2();
		bean.field1.field2 = "value2";
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<object1><object2><elem2>value2</elem2></object2></object1>";
		assertEquals(exp, xml);
	}

	@XmlRootElement(name = "object1")
	private static class ObjectRefClass1 {
		@XmlElementRef
		public ObjectRefClass2 field1;
	}
	@XmlRootElement(name = "object2")
	private static class ObjectRefClass2 {
		@XmlElement(name = "elem2")
		public String field2;
	}

	@Test
	public void testStringList() throws Exception {
		StringListClass1 bean = new StringListClass1();
		bean.field1 = new ArrayList<>();
		bean.field1.add("value1");
		bean.field1.add("value2");
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<root><elem>value1</elem><elem>value2</elem></root>";
		assertEquals(exp, xml);
	}

	@XmlRootElement(name = "root")
	private static class StringListClass1 {
		@XmlElement(name = "elem")
		public List<String> field1;
	}

	@Test
	public void testObjectList() throws Exception {
		ObjectListClass1 bean = new ObjectListClass1();
		bean.field1 = new ArrayList<>();
		ObjectListClass2 value1 = new ObjectListClass2();
		value1.field2 = "value1";
		bean.field1.add(value1);
		ObjectListClass2 value2 = new ObjectListClass2();
		value2.field2 = "value2";
		bean.field1.add(value2);
		String xml = xmlGenerator.generate(bean);
		String exp = XMLDECL + "<object1><object2><elem2>value1</elem2></object2><object2><elem2>value2</elem2></object2></object1>";
		assertEquals(exp, xml);
	}

	@XmlRootElement(name = "object1")
	private static class ObjectListClass1 {
		@XmlElementRef
		public List<ObjectListClass2> field1;
	}
	@XmlRootElement(name = "object2")
	private static class ObjectListClass2 {
		@XmlElement(name = "elem2")
		public String field2;
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testMissingClassAnnotation() throws Exception {
		Object bean = new Object();
		xmlGenerator.generate(bean);
	}

	@Test(expectedExceptions = ServiceException.class)
	public void testMissingFieldClassAnnotation() throws Exception {
		MissingFieldAnnoClass bean = new MissingFieldAnnoClass();
		bean.field = new Object(); // must not be null
		xmlGenerator.generate(bean);
	}

	@XmlRootElement
	private static class MissingFieldAnnoClass {
		@XmlElement
		public Object field;
	}
}
