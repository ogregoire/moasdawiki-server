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

package net.moasdawiki.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.moasdawiki.base.ServiceException;

/**
 * Liest einen HTTP-Request gemäß RFC 2616 (HTTP/1.1) ein, parst den HTTP-Header
 * und liest den HTTP-Body ein. Als Zeichenkodierung wird UTF-8 vorausgesetzt.
 * <br>
 * <br>
 * Beispiel:
 * 
 * <pre>
 * GET /index.html HTTP/1.1
 * Host: www.moasdawiki.net
 * </pre>
 * 
 * Diese Klasse ist Thread-safe.
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public abstract class HttpRequestParser {

	/**
	 * Liest den HTTP-Request ein.
	 * 
	 * @param inputStream Eingabestrom zum Lesen der Request-Daten. Nicht
	 *        <code>null</code>.
	 * @return HTTP-Anfragedaten. Nicht <code>null</code>.
	 * @throws ServiceException wenn ein Fehler auftrat
	 */
	public static HttpRequest parse(InputStream inputStream) throws ServiceException {
		HttpRequest http = new HttpRequest();

		Map<String, String> httpHeader = readHttpHeader(inputStream);
		http.httpHeader = httpHeader;

		// Request-Line zerlegen
		String requestLine = httpHeader.get(HttpRequest.HTTP_HEADER_FIRST_LINE);
		String method = extractMethod(requestLine);
		http.method = method;
		String url = extractUrl(requestLine);
		http.url = url;
		http.urlPath = extractUrlPath(url);
		http.urlParameters = extractUrlParameters(url);

		// HTTP-Body einlesen
		String contentLengthStr = httpHeader.get(HttpRequest.HTTP_HEADER_CONTENT_LENGTH);
		byte[] httpBody = readHttpBody(contentLengthStr, inputStream);
		http.httpBody = httpBody;

		// POST-Formulardaten einlesen
		String contentType = httpHeader.get(HttpRequest.HTTP_HEADER_CONTENT_TYPE);
		if ("POST".equals(method) && "application/x-www-form-urlencoded".equals(contentType)) {
			http.urlParameters = extractPostData(httpBody);
		}

		return http;
	}

	/**
	 * Liest den HTTP-Header ein.
	 */
	private static Map<String, String> readHttpHeader(InputStream is) throws ServiceException {
		Map<String, String> result = new HashMap<>();
		try {
			while (true) {
				String line = readLine(is);
				if (line.isEmpty()) {
					// Leerzeile beendet HTTP-Header
					break;
				} else if (result.isEmpty()) {
					// Sonderbehandlung für erste Headerzeile
					result.put(HttpRequest.HTTP_HEADER_FIRST_LINE, line);
				} else {
					// Headerzeile hat das Format "Name: Wert"
					int pos = line.indexOf(':');
					if (pos > 0) {
						// case-insensitive --> in Kleinbuchstaben umwandeln
						String name = line.substring(0, pos).trim().toLowerCase();
						String value = line.substring(pos + 1).trim();
						result.put(name, value);
					} else {
						// ungültiges Format, ignorieren
						System.out.println("Invalid HTTP header: " + line);
					}
				}
			}
			if (result.isEmpty()) {
				throw new ServiceException("Empty HTTP header");
			}
			return result;
		} catch (Exception e) {
			throw new ServiceException("Error reading HTTP headers", e);
		}
	}

	/**
	 * Liest eine Zeile von Eingabestrom. Gemäß RFC 2616 werden als Zeilenende
	 * \n und \r\n akzeptiert. Erkennt nicht das Ende des Stroms, darf also nur
	 * für die Headerzeilen verwendet werden.
	 */
	private static String readLine(InputStream is) throws IOException {
		StringBuilder line = new StringBuilder();
		int b;
		while ((b = is.read()) >= 0) {
			//noinspection StatementWithEmptyBody
			if (b == '\r') {
				// Zeichen ignorieren
			} else if (b == '\n') {
				break; // Zeilenende erreicht
			} else {
				// Zeichen hinzufügen
				line.append((char) b);
			}
		}
		return line.toString();
	}

	/**
	 * Ermittelt die Abfragemethode, z.B. <tt>GET</tt>.
	 */
	private static String extractMethod(String requestLine) throws ServiceException {
		int pos = requestLine.indexOf(' ');
		if (pos > 0) {
			return requestLine.substring(0, pos);
		} else {
			throw new ServiceException("Invalid first HTTP header row: " + requestLine);
		}
	}

	/**
	 * Bestimmt die URL der Abfrage. Diese steht in der ersten Zeile des
	 * Requests.<br>
	 * <br>
	 * Beispiel:
	 * <tt>GET /pfad/action?param1=value1&amp;param2=value2 HTTP/1.1</tt><br>
	 * URL: <tt>/pfad/action?param1=value1&amp;param2=value2</tt>
	 */
	private static String extractUrl(String requestLine) throws ServiceException {
		int pos1 = requestLine.indexOf(' ');
		int pos2 = requestLine.indexOf(' ', pos1 + 1);
		if (pos1 < 0 || pos2 < 0) {
			throw new ServiceException("Invalid first HTTP header row: " + requestLine);
		}
		String encodedUrl = requestLine.substring(pos1 + 1, pos2);

		try {
			// URL dekodieren, bei %xy-Angaben UTF-8 berücksichtigen
			return URLDecoder.decode(encodedUrl, "UTF-8");
		} catch (Exception e) {
			throw new ServiceException("Invalid request URL: " + encodedUrl, e);
		}
	}

	/**
	 * Bestimmt den Pfadteil der URL, ohne "?" und nachfolgende Parameter.<br>
	 * <br>
	 * Beispiel:<br>
	 * URL: <tt>/pfad/action?param1=value1&amp;param2=value2</tt><br>
	 * URL-Pfad: <tt>/pfad/action</tt>
	 */
	private static String extractUrlPath(String url) throws ServiceException {
		int pos = url.indexOf('?');
		String result;
		if (pos < 0) {
			// URL hat keine Parameter
			result = url;
		} else {
			// Parameter abschneiden
			result = url.substring(0, pos);
		}
		if (result.isEmpty()) {
			throw new ServiceException("Invalid request URL, path is empty: " + url);
		}
		return result;
	}

	/**
	 * Gibt die Abfrage-Parameter in der URL zurück.<br>
	 * <br>
	 * Beispiel:<br>
	 * URL: <tt>/pfad/action?param1=value1&amp;param2=value2</tt><br>
	 * Abfrage-Parameter: <tt>param1=value1</tt>, <tt>param2=value2</tt>
	 */
	private static Map<String, String> extractUrlParameters(String url) throws ServiceException {
		int pos = url.indexOf('?');
		if (pos < 0) {
			// keine Parameter vorhanden
			return Collections.emptyMap();
		}
		return parseUrlParameters(url.substring(pos + 1));
	}

	/**
	 * Zerlegt einen Parameterstring in Name-Wert-Paare.<br>
	 * <br>
	 * Beispiel:<br>
	 * Parameterstring: <tt>param1=value1&amp;param2=value2</tt><br>
	 * Name-Wert-Paare: <tt>param1=value1</tt>, <tt>param2=value2</tt>
	 */
	private static Map<String, String> parseUrlParameters(String queryStr) throws ServiceException {
		Map<String, String> result = new HashMap<>();
		try {
			int index = 0;
			while (index < queryStr.length()) {
				String entry;
				int separatorpos = queryStr.indexOf("&", index);
				if (separatorpos >= index) {
					entry = queryStr.substring(index, separatorpos);
					index = separatorpos + 1;
				} else {
					entry = queryStr.substring(index);
					index = queryStr.length(); // Ende der Post-Daten erreicht
				}

				int middlepos = entry.indexOf("=");
				if (middlepos >= 0) {
					String name = URLDecoder.decode(entry.substring(0, middlepos), "UTF-8");
					String value = URLDecoder.decode(entry.substring(middlepos + 1), "UTF-8");
					result.put(name, value);
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new ServiceException("Error reading HTTP POST data", e);
		}
		return result;
	}

	/**
	 * Liest den HTTP-Body ein. Der InputStream muss bereits an der richtigen
	 * Stelle stehen, d.h. der HTTP-Header ist bereits eingelesen.
	 */
	private static byte[] readHttpBody(String contentLengthStr, InputStream is) throws ServiceException {
		if (contentLengthStr == null) {
			// kein Body vorhanden
			return new byte[0];
		}

		int contentLength;
		try {
			contentLength = Integer.parseInt(contentLengthStr);
		} catch (Exception e) {
			throw new ServiceException("Invalid Content-Length: " + contentLengthStr, e);
		}

		try {
			byte[] httpBody = new byte[contentLength];
			int count = 0;
			int read;
			while (count < contentLength && (read = is.read(httpBody, count, contentLength - count)) >= 0) {
				count += read;
			}
			return httpBody;
		} catch (IOException e) {
			throw new ServiceException("Error reading HTTP body data", e);
		}
	}

	/**
	 * Wertet die POST-Parameter im HTTP-Body aus.
	 */
	private static Map<String, String> extractPostData(byte[] httpBody) throws ServiceException {
		try {
			// in String umwandeln
			String postData = new String(httpBody, "UTF-8");
			return parseUrlParameters(postData);
		} catch (UnsupportedEncodingException e) {
			throw new ServiceException("Error reading HTTP POST data", e);
		}
	}
}
