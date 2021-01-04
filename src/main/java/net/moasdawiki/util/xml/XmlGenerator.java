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

import java.lang.reflect.Field;
import java.util.List;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Erzeugt einen XML-Strom aus einer Bean. Implementiert JAXB rudimentär nach,
 * weil es in Android nicht verfügbar ist. Über Annotationen können die
 * XML-Tagnamen und -Attributnamen gesteuert werden. Der XMl-Strom muss im
 * UTF-8-Encoding sein.
 */
public class XmlGenerator {
	private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private StringBuilder xml;

	/**
	 * Konstruktor.
	 */
	public XmlGenerator(@SuppressWarnings("unused") Logger logger) {
	}

	/**
	 * Erzeugt aus dem angegebenen Objekt einen XML-Strom. Über Annotationen
	 * können die XML-Tagnamen und -Attributnamen gesteuert werden.
	 * 
	 * @param bean JAXB-Bean, die in XML serialisiert werden soll. Nicht
	 *        <code>null</code>.
	 * @return XML-Strom. Nicht <code>null</code>.
	 */
	@NotNull
	public String generate(@NotNull Object bean) throws ServiceException {
		xml = new StringBuilder();
		xml.append(XML_DECLARATION);
		processObject(bean);
		return xml.toString();
	}

	private void processObject(@NotNull Object bean) throws ServiceException {
		// Wurzel-Tag
		XmlRootElement xmlRoot = bean.getClass().getAnnotation(XmlRootElement.class);
		if (xmlRoot == null) {
			throw new ServiceException("XmlRootElement annotation missing on class '" + bean.getClass().getSimpleName() + "'");
		}
		String rootTag = xmlRoot.name();
		if (rootTag.length() == 0) {
			rootTag = bean.getClass().getSimpleName();
		}
		xml.append('<');
		xml.append(rootTag);

		// Attribute
		for (Field field : bean.getClass().getFields()) {
			XmlAttribute attr = field.getAnnotation(XmlAttribute.class);
			Object value = getFieldValue(field, bean);
			if (attr != null && value != null) {
				processAttribute(attr, field.getName(), value);
			}
		}
		xml.append('>');

		// Wert des Root-Tags
		for (Field field : bean.getClass().getFields()) {
			XmlValue attr = field.getAnnotation(XmlValue.class);
			Object value = getFieldValue(field, bean);
			if (attr != null && value != null) {
				processValue(value);
			}
		}

		// Elemente
		for (Field field : bean.getClass().getFields()) {
			XmlElement elem = field.getAnnotation(XmlElement.class);
			XmlElementRef elemRef = field.getAnnotation(XmlElementRef.class);
			Object value = getFieldValue(field, bean);
			if (elem != null && value != null) {
				processElement(elem, field.getName(), value);
			} else if (elemRef != null && value != null) {
				processElementRef(value);
			}
		}

		xml.append("</");
		xml.append(rootTag);
		xml.append('>');
	}

	private void processAttribute(@NotNull XmlAttribute attribute, @NotNull String fieldName, @NotNull Object fieldValue) {
		String attrName = attribute.name();
		if (attrName.length() == 0) {
			attrName = fieldName;
		}
		xml.append(' ');
		xml.append(attrName);
		xml.append("=\"");
		xml.append(EscapeUtils.escapeHtml(fieldValue.toString()));
		xml.append('"');
	}

	private void processValue(@NotNull Object fieldValue) {
		xml.append(EscapeUtils.escapeHtml(fieldValue.toString()));
	}

	private void processElement(@NotNull XmlElement element, @NotNull String fieldName, @NotNull Object fieldValue) throws ServiceException {
		String tagName = element.name();
		if (tagName.length() == 0) {
			tagName = fieldName;
		}
		if (fieldValue instanceof String || fieldValue instanceof Boolean) {
			xml.append('<');
			xml.append(tagName);
			xml.append('>');
			xml.append(EscapeUtils.escapeHtml(fieldValue.toString()));
			xml.append("</");
			xml.append(tagName);
			xml.append('>');
		} else if (fieldValue instanceof List) {
			List<?> list = (List<?>) fieldValue;
			for (Object entry : list) {
				xml.append('<');
				xml.append(tagName);
				xml.append('>');
				if (entry instanceof String) {
					xml.append(EscapeUtils.escapeHtml((String) entry));
				}
				xml.append("</");
				xml.append(tagName);
				xml.append('>');
			}
		} else {
			xml.append('<');
			xml.append(tagName);
			xml.append('>');
			processObject(fieldValue);
			xml.append("</");
			xml.append(tagName);
			xml.append('>');
		}
	}

	private void processElementRef(@NotNull Object fieldValue) throws ServiceException {
		if (fieldValue instanceof List) {
			List<?> list = (List<?>) fieldValue;
			for (Object entry : list) {
				processObject(entry);
			}
		} else {
			processObject(fieldValue);
		}
	}

	private Object getFieldValue(@NotNull Field field, @NotNull Object bean) throws ServiceException {
		try {
			return field.get(bean);
		} catch (Exception e) {
			throw new ServiceException("Cannot get field value", e);
		}
	}
}
