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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parst einen XML-Strom und füllt die Daten in die angegebene Bean.
 * Implementiert JAXB rudimentär nach, weil es in Android nicht verfügbar ist.
 * Über Annotationen können die XML-Tagnamen und -Attributnamen gesteuert
 * werden. Der XMl-Strom muss im UTF-8-Encoding sein.
 */
public class XmlParser {
	@NotNull
	private final Logger logger;

	private String xml;
	private int charsRead;

	/**
	 * Konstruktor.
	 */
	public XmlParser(@NotNull Logger logger) {
		this.logger = logger;
	}

	/**
	 * Parst einen XML-Strom und füllt die Daten in ein neues Objekt vom
	 * angegebenen Typ.
	 */
	@NotNull
	public <T> T parse(@NotNull String xml, @NotNull Class<T> beanType) throws ServiceException {
		this.xml = xml;
		charsRead = 0;
		readDeclaration();

		T bean;
		try {
			bean = beanType.getConstructor().newInstance();
		} catch (Exception e) {
			throw new ServiceException("Cannot create bean instance", e);
		}
		parseRootElement(bean);
		return bean;
	}

	/**
	 * Konsumiert den XML-Prolog "<?xml ... ?>".
	 */
	private void readDeclaration() throws ServiceException {
		if (!xml.startsWith("<?xml", charsRead)) {
			// kein Header vorhanden
			return;
		}
		int end = xml.indexOf("?>", charsRead);
		if (end == -1) {
			throw new ServiceException("Declaration end ?> expected");
		}
		charsRead += end + 2;
	}

	/**
	 * Liest ein XML-Element samt enthaltener Unter-Elemente ein.
	 * 
	 * @param bean Zu füllende Bean. <code>null</code> --> nur XML konsumieren.
	 */
	private void parseRootElement(@Nullable Object bean) throws ServiceException {
		// Elementname einlesen
		readToken("<");
		int pos = charsRead;
		String elementName = nextTokenIgnoreWhitespace();
		if (elementName == null) {
			throw new ServiceException("Token expected after '<' but found EOF");
		}
		if (bean != null) {
			String rootAnnotationName = getRootAnnotationName(bean.getClass());
			if (rootAnnotationName == null) {
				throw new ServiceException("Missing XmlRootElement annotation in class '" + bean.getClass().getSimpleName() + "'");
			}
			if (!rootAnnotationName.equals(elementName)) {
				throw new ServiceException("XML element '" + rootAnnotationName + "' expected, but found '" + elementName + "' at position " + pos);
			}
		}

		// Attribute einlesen
		parseAttributes(bean);

		// Tag-Ende einlesen
		String endToken = nextTokenIgnoreWhitespace();
		if (!">".equals(endToken) && !"/".equals(endToken)) {
			throw new ServiceException("'>' or '/>' expected but found '" + endToken + "' at end of element '" + elementName + "'");
		}
		if ("/".equals(endToken)) {
			String endToken2 = nextTokenIgnoreWhitespace();
			if (!">".equals(endToken2)) {
				throw new ServiceException("'/>' expected but found '/" + endToken2 + "' at end of element '" + elementName + "'");
			}
			// Element ist bereits zu Ende
			return;
		}

		// Wert des Root-Tags einlesen
		parseRootTagValue(bean);

		// Sub-Elemente einlesen
		parseElements(bean);

		// Ende-Tag lesen
		readToken("<");
		readToken("/");
		readToken(elementName);
		readToken(">");
	}

	/**
	 * Liest alle Attribute eines Elements ein.
	 */
	private void parseAttributes(@Nullable Object bean) throws ServiceException {
		while (true) {
			int pos = charsRead;
			String attributeName = nextTokenIgnoreWhitespace();
			if (">".equals(attributeName) || "/".equals(attributeName) || attributeName == null) {
				// Tag-Ende erreicht, keine weiteren Attribute mehr
				charsRead = pos;
				break;
			}
			readToken("=");
			String attributeValue = parseAttributeValue();
			Field field = getBeanField(bean, FieldAnnotation.ATTRIBUTE, attributeName);
			setBeanFieldValue(bean, field, attributeValue);
		}
	}

	/**
	 * Liest einen Attributwert ein. Dieser kann optional mit Anführungszeichen
	 * (") umschlossen sein, diese werden automatisch entfernt.
	 *
	 * @return Attributwert, nicht null.
	 */
	@NotNull
	private String parseAttributeValue() {
		int pos = charsRead;
		String token = nextTokenIgnoreWhitespace();
		if (token == null) {
			// Stromende, kein Attributwert vorhanden
			return "";
		} else if ("\"".equals(token)) {
			// Text in Anführungszeichen einlesen
			StringBuilder sb = new StringBuilder();
			token = nextToken();
			while (token != null && !"\"".equals(token)) {
				sb.append(token);
				token = nextToken();
			}
			return unescapeXmlValue(sb.toString());
		} else if (isXmlSpecialCharacter(token.charAt(0))) {
			// XML-Sonderzeichen --> kein Attributwert vorhanden
			charsRead = pos; // Token nicht konsumieren
			return "";
		} else {
			// Text ohne Anführungszeichen
			return unescapeXmlValue(token);
		}
	}

	/**
	 * Liest den Wert des Root-Tags ein.
	 */
	private void parseRootTagValue(@Nullable Object bean) throws ServiceException {
		String value = parseTagValue();
		if (value != null) {
			Field field = getBeanField(bean, FieldAnnotation.VALUE, null);
			setBeanFieldValue(bean, field, value);
		}
	}

	/**
	 * Liest den Wert eines Tags ein.
	 * 
	 * @return Text, <code>null</code> --> kein Text vorhanden.
	 */
	@Nullable
	private String parseTagValue() {
		StringBuilder sb = new StringBuilder();
		int pos = charsRead;
		String token = nextToken();
		while (token != null && !"<".equals(token)) {
			sb.append(token);
			pos = charsRead;
			token = nextToken();
		}
		if ("<".equals(token)) {
			// Textende erreicht, letztes Token nicht konsumieren
			charsRead = pos;
		}

		if (sb.length() > 0) {
			// white-space außen herum wegschneiden
			String value = sb.toString().trim();
			return unescapeXmlValue(value);
		}
		return null; // kein Text
	}

	/**
	 * Liest alle Sub-Elemente eines Elements ein.
	 */
	private void parseElements(@Nullable Object bean) throws ServiceException {
		//noinspection StatementWithEmptyBody
		while (parseElement(bean)) {
		}
	}

	/**
	 * Liest ein Sub-Element eines Elements ein.
	 */
	private boolean parseElement(@Nullable Object bean) throws ServiceException {
		// Elementname ermitteln
		int pos = charsRead;
		readToken("<");
		String elementName = nextTokenIgnoreWhitespace();
		if (elementName == null) {
			throw new ServiceException("Token expected after '<' but found EOF");
		}
		if ("/".equals(elementName)) {
			// übergeordnetes Element ist zu Ende
			charsRead = pos;
			return false;
		}

		// Typ des Elements in der Bean ermitteln, unterstützt auch Listen
		Field field = getBeanField(bean, FieldAnnotation.ELEMENT, elementName);
		Class<?> fieldType = getBeanFieldType(field);
		if (fieldType == null) {
			// Element konsumieren
			charsRead = pos;
			parseRootElement(null);
		} else if (fieldType.isAssignableFrom(String.class) || fieldType.isAssignableFrom(Boolean.class)) {
			// String-Feld, direkt parsen
			String value = parseElementString(elementName);
			setBeanFieldValue(bean, field, value);
		} else {
			// Sub-Bean parsen
			// Unterstützt nur XmlElementRef, keine zusätzliche Element-Klammer
			Object subBean;
			try {
				subBean = fieldType.getConstructor().newInstance();
			} catch (Exception e) {
				throw new ServiceException(
						"Error creating instance for field '" + field.getName() + "' in bean class '" + bean.getClass().getSimpleName() + "'", e);
			}
			charsRead = pos;
			parseRootElement(subBean);
			setBeanFieldValue(bean, field, subBean);
		}
		return true;
	}

	/**
	 * Liest den String-Wert eines Elements ein. Der Tagname wurde bereits
	 * eingelesen, der Rest noch nicht.
	 * 
	 * return String-Wert. <code>null</code> --> kein Wert vorhanden.
	 */
	@Nullable
	private String parseElementString(@NotNull String elementName) throws ServiceException {
		// Attribute ignorieren
		parseAttributes(null);

		// Tag-Ende einlesen
		String endToken = nextTokenIgnoreWhitespace();
		if (!">".equals(endToken) && !"/".equals(endToken)) {
			throw new ServiceException("'>' or '/>' expected but found '" + endToken + "' at end of element '" + elementName + "'");
		}
		if ("/".equals(endToken)) {
			String endToken2 = nextTokenIgnoreWhitespace();
			if (!">".equals(endToken2)) {
				throw new ServiceException("'/>' expected but found '/" + endToken2 + "' at end of element '" + elementName + "'");
			}
			// Element ist bereits zu Ende
			return null;
		}

		// String-Wert einlesen, inkl. Zeilenumbrüche
		String value = parseTagValue();

		// Ende-Tag lesen
		readToken("<");
		readToken("/");
		readToken(elementName);
		readToken(">");
		return value;
	}

	/**
	 * Liest das nächste Token ein und wirft eine Exception, wenn es nicht der
	 * Erwartung entspricht.
	 */
	private void readToken(@NotNull String expectedToken) throws ServiceException {
		int pos = charsRead;
		String token = nextTokenIgnoreWhitespace();
		if (!expectedToken.equals(token)) {
			throw new ServiceException("Token '" + expectedToken + "' expected but found '" + token + "' at position " + pos);
		}
	}

	/**
	 * Liest das nächste Token ein. Leerzeichen, Tabs und Zeilenumbrüche werden
	 * ignoriert.
	 *
	 * @return Nächstes Token, enthält mindestens ein Zeichen. null --> Ende des
	 *         XML-Stroms erreicht.
	 */
	@Nullable
	private String nextTokenIgnoreWhitespace() {
		String token;
		do {
			token = nextToken();
			// white-space ignorieren
		} while (token != null && !token.isEmpty() && Character.isWhitespace(token.charAt(0)));
		return token;
	}

	/**
	 * Liest das nächste Token ein. Tokenarten:
	 * <ul>
	 * <li>XML-Sonderzeichen: <,>,/,= (nur 1 Zeichen)</li>
	 * <li>White-space (mehrere Zeichen am Stück möglich)</li>
	 * <li>Sonstiger Text (mehrere Zeichen am Stück möglich)</li>
	 * </ul>
	 * 
	 * @return Nächstes Token, enthält mindestens ein Zeichen. null --> Ende des
	 *         XML-Stroms erreicht.
	 */
	@Nullable
	private String nextToken() {
		TokenType tokenType = TokenType.UNKNOWN;
		StringBuilder sb = new StringBuilder();
		loop: while (charsRead < xml.length()) {
			char ch = xml.charAt(charsRead);
			switch (tokenType) {
			case UNKNOWN:
				// Erstes Tokenzeichen auf jeden Fall übernehmen
				sb.append(ch);
				charsRead++;

				// Tokentyp ermitteln, nur beim ersten Zeichen relevant
				if (Character.isWhitespace(ch)) {
					tokenType = TokenType.WHITE_SPACE;
				} else if (isXmlSpecialCharacter(ch)) {
					// Tokenlänge nur 1 Zeichen
					break loop;
				} else {
					tokenType = TokenType.TEXT;
				}
				break;

			case WHITE_SPACE:
				if (!Character.isWhitespace(ch)) {
					// white-space Token ist zu Ende
					break loop;
				}
				sb.append(ch);
				charsRead++;
				break;

			case TEXT:
				if (Character.isWhitespace(ch) || isXmlSpecialCharacter(ch)) {
					// String-Token ist zu Ende
					break loop;
				}
				sb.append(ch);
				charsRead++;
			}
		}

		if (sb.length() == 0) {
			// kein Token mehr vorhanden, Text zu Ende
			return null;
		}
		return sb.toString();
	}

	private enum TokenType {
		WHITE_SPACE, TEXT, UNKNOWN
	}

	private boolean isXmlSpecialCharacter(char ch) {
		return ch == '<' || ch == '>' || ch == '/' || ch == '=' || ch == '"';
	}

	/**
	 * Gibt den XML-Elementnamen aus der {@link XmlRootElement}-Annotation
	 * zurück.
	 * 
	 * @return XML-Elementname. <code>null</code> --> keine
	 *         {@link XmlRootElement}-Annotation vorhanden.
	 */
	@Nullable
	private String getRootAnnotationName(@NotNull Class<?> clazz) {
		XmlRootElement rootAnnotation = clazz.getAnnotation(XmlRootElement.class);
		if (rootAnnotation == null) {
			// Annotation nicht vorhanden
			return null;
		}
		if (rootAnnotation.name().length() > 0) {
			return rootAnnotation.name();
		} else {
			return clazz.getSimpleName();
		}
	}

	private enum FieldAnnotation {
		ATTRIBUTE, ELEMENT, VALUE
	}

	/**
	 * Gibt das Feld einer JAXB-Bean zurück, das zum angegebenen
	 * XML-Elementnamen passt.
	 *
	 * @return Bean-Feld. <code>null</code> --> kein passendes Feld gefunden.
	 */
	@Nullable
	private Field getBeanField(@Nullable Object bean, @NotNull FieldAnnotation fieldAnnotation, @Nullable String fieldName) {
		if (bean == null) {
			return null;
		}
		for (Field field : bean.getClass().getFields()) {
			// XML-Elementname aus XML-Annotation ermitteln
			XmlAttribute attributeAnnotation = field.getAnnotation(XmlAttribute.class);
			XmlValue valueAnnotation = field.getAnnotation(XmlValue.class);
			XmlElement elementAnnotation = field.getAnnotation(XmlElement.class);
			XmlElementRef elementRefAnnotation = field.getAnnotation(XmlElementRef.class);
			String annotationFieldName = null;
			if (fieldAnnotation == FieldAnnotation.ATTRIBUTE && attributeAnnotation != null) {
				if (attributeAnnotation.name().length() > 0) {
					annotationFieldName = attributeAnnotation.name();
				} else {
					annotationFieldName = field.getName();
				}
			} else if (fieldAnnotation == FieldAnnotation.ELEMENT && elementAnnotation != null) {
				if (elementAnnotation.name().length() > 0) {
					annotationFieldName = elementAnnotation.name();
				} else {
					annotationFieldName = field.getName();
				}
			} else if (fieldAnnotation == FieldAnnotation.ELEMENT && elementRefAnnotation != null) {
				// RootAnnotation aus Feldtyp holen, auch Listen berücksichtigen
				Class<?> fieldType = field.getType();
				if (fieldType.isAssignableFrom(List.class)) {
					// Typ aus Liste ermitteln
					ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
					fieldType = (Class<?>) stringListType.getActualTypeArguments()[0];
				}
				String rootAnnotationName = getRootAnnotationName(fieldType);
				if (rootAnnotationName != null) {
					annotationFieldName = rootAnnotationName;
				}
			}

			if (fieldAnnotation == FieldAnnotation.VALUE && valueAnnotation != null || annotationFieldName != null && annotationFieldName.equals(fieldName)) {
				return field;
			}
		}
		logger.write("Warning: Bean '" + bean.getClass().getSimpleName() + "' has no field for XML element or attribute '" + fieldName + "'");
		return null;
	}

	/**
	 * Gibt den Bean-Typ eines Felds zurück. Wenn das Feld eine Liste ist, wird
	 * der Typ der Listeneinträge zurückgegeben. Die Typklasse muss die
	 * Annotation {@link XmlRootElement} haben, außer es handelt sich um
	 * {@link String}.
	 * 
	 * @return Bean-Typ. <code>null</code> --> kein unterstützter Typ.
	 */
	@Nullable
	private Class<?> getBeanFieldType(@Nullable Field field) throws ServiceException {
		if (field == null) {
			return null;
		}

		Class<?> fieldType = field.getType();
		if (fieldType.isAssignableFrom(List.class)) {
			// Typ aus Liste ermitteln
			ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
			fieldType = (Class<?>) stringListType.getActualTypeArguments()[0];
		}
		if (fieldType.isAssignableFrom(String.class) || fieldType.isAssignableFrom(Boolean.class)) {
			// keine Annotation notwendig
			return fieldType;
		}

		// XmlRootElement-Annotation prüfen
		XmlRootElement rootElementAnnotation = fieldType.getAnnotation(XmlRootElement.class);
		if (rootElementAnnotation == null) {
			throw new ServiceException("Missing XML annotation on bean class '" + fieldType.getSimpleName() + "'");
		}
		return fieldType;
	}

	/**
	 * Übernimmt einen XML-Wert in das zugehörige Bean-Feld. Der Wert muss den
	 * Typ haben, der von {@link #getBeanFieldType(Field)} zurückgegeben wurde.
	 * Wenn es sich beim betroffenen Bean-Feld um eine Liste handelt, wird diese
	 * ggf. automatisch erstellt und der Wert eingefügt. Ist kein passendes Feld
	 * vorhanden oder das Feld ist keine Liste und hat bereits einen Wert, wird
	 * der Aufruf ignoriert.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setBeanFieldValue(@Nullable Object bean, @Nullable Field field, @Nullable Object newValue) throws ServiceException {
		if (bean == null || field == null || newValue == null) {
			return;
		}

		// Typ und aktuellen Wert holen
		Class<?> fieldClass = field.getType();
		Object fieldValue;
		try {
			fieldValue = field.get(bean);
		} catch (Exception e) {
			throw new ServiceException("Error accessing field '" + field.getName() + "' in bean class '" + bean.getClass().getSimpleName() + "'", e);
		}

		// Neuen Wert setzen
		if (fieldClass.isAssignableFrom(List.class)) {
			// Es handelt sich um eine Liste
			if (fieldValue == null) {
				// Leere Liste erstellen
				ArrayList<Object> fieldList = new ArrayList<>();
				try {
					field.set(bean, fieldList);
				} catch (IllegalAccessException e) {
					throw new ServiceException("Error assigning field '" + field.getName() + "' in bean class '" + bean.getClass().getSimpleName() + "'", e);
				}
				fieldValue = fieldList;
			}
			// Neuen Wert einfügen
			((List) fieldValue).add(newValue);
		} else if (fieldClass.isAssignableFrom(Boolean.class)) {
			// Es handelt sich um einen Boolean-Wert
			if (fieldValue != null) {
				// Feld hat bereits einen Wert, nicht überschreiben
				logger.write("Field '" + field.getName() + "' in bean class '" + bean.getClass().getSimpleName() + "' has already a value");
				return;
			}
			try {
				Boolean boolValue = Boolean.valueOf((String) newValue);
				field.set(bean, boolValue);
			} catch (Exception e) {
				throw new ServiceException("Error assigning Boolean field '" + field.getName() + "' in bean class '"
						+ bean.getClass().getSimpleName() + "' with value '" + newValue + "'", e);
			}
		} else {
			// Kann nur ein String sein
			if (fieldValue != null) {
				// Feld hat bereits einen Wert, nicht überschreiben
				logger.write("Field '" + field.getName() + "' in bean class '" + bean.getClass().getSimpleName() + "' has already a value");
				return;
			}
			try {
				field.set(bean, newValue);
			} catch (IllegalAccessException e) {
				throw new ServiceException("Error assigning field '" + field.getName() + "' in bean class '" + bean.getClass().getSimpleName() + "'", e);
			}
		}
	}

	/**
	 * Ersetzt XML-escapete Zeichen durch das ursprüngliche Zeichen, z.B.
	 * "&amp;" --> "&".
	 * 
	 * @param value Text mit escapeten Zeichen. Nicht <code>null</code>.
	 * @return Text ohne escapete Zeichen. Nicht <code>null</code>.
	 */
	@NotNull
	String unescapeXmlValue(@NotNull String value) {
		if (value.indexOf('&') < 0) {
			return value;
		}
		StringBuilder sb = new StringBuilder(value);
		int pos = sb.indexOf("&");
		while (pos >= 0) {
			int pos2 = sb.indexOf(";", pos);
			if (pos2 > pos) {
				String escapedChar = sb.substring(pos, pos2 + 1);
				String unescapedChar = unescapeXmlChar(escapedChar);
				sb.replace(pos, pos2 + 1, unescapedChar);
			}
			// nächstes Zeichen suchen
			pos = sb.indexOf("&", pos + 1);
		}
		return sb.toString();
	}

	@NotNull
	String unescapeXmlChar(@NotNull String escapedChar) {
		switch (escapedChar) {
		case "&lt;":
			return "<";
		case "&gt;":
			return ">";
		case "&amp;":
			return "&";
		case "&quot;":
			return "\"";
		case "&apos;":
			return "'";
		default:
			// unbekanntes Zeichen --> unverändert lassen
			return escapedChar;
		}
	}
}
