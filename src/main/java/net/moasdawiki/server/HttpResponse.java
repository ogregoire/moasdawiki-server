/*
 * Copyright (c) 2008 - 2019 Dr. Herbert Reiter (support@moasdawiki.net)
 * 
 * This file is part of MoasdaWiki.
 * 
 * MoasdaWiki is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * MoasdaWiki is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MoasdaWiki. If not, see <http://www.gnu.org/licenses/>.
 */

package net.moasdawiki.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import net.moasdawiki.base.ServiceException;
import net.moasdawiki.util.EscapeUtils;

/**
 * Erzeugt eine HTTP-Antwort. Diese besteht aus einem HTTP-Header und ggf. dem
 * Body-Abschnitt.
 * 
 * @author Herbert Reiter
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public class HttpResponse {
	public static final String CONTENT_TYPE_HTML = "text/html";
	public static final String CONTENT_TYPE_XML = "text/xml";
	public static final String CONTENT_TYPE_TEXT = "text/plain";
	public static final String CONTENT_TYPE_BINARY = "application/octet-stream";
	public static final String CONTENT_TYPE_JSON_UTF8 = "application/json; charset=utf-8";

	private static final String CRLF = "\r\n";
	private int statusCode;
	private String redirectUrl;
	private String contentType;
	private byte[] content; // HTTP-Body in Maschinendarstellung; null = leer
	private boolean cacheable; // false -> no-cache angeben

	/**
	 * Konstruktor.
	 */
	public HttpResponse() {
		statusCode = 200;
		contentType = CONTENT_TYPE_HTML;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Setzt eine Redirect-URL, die in den HTTP-Header geschrieben wird.
	 * 
	 * @param redirectUrl URL, an die weitergeleitet werden soll.
	 */
	public void setRedirectUrl(String redirectUrl) {
		setStatusCode(302);
		this.redirectUrl = redirectUrl;
	}

	/**
	 * Setzt den Content-Type des Ausgabestroms. Standardwert ist "text/html".
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}

	/**
	 * Übernimmt Binärdaten in den Ausgabestrom.<br>
	 * <br>
	 * In diesem Fall wird das no-cache-Flag im Header nicht angegeben, weil
	 * sonst der Internet Explorer das Herunterladen der Datei verweigert, siehe
	 * http://support.microsoft.com/default.aspx?scid=kb;en-us;323308
	 */
	public void setContent(byte[] content) {
		this.content = content;
		cacheable = true; // Dateien dürfen gecacht werden, weil sonst der IE
		// Probleme hat
	}

	/**
	 * Übernimmt einen Text im UTF-8-Format in den Ausgabestrom.
	 * 
	 * @param content Text. Nicht <code>null</code>.
	 */
	public void setContent(String content) {
		try {
			this.content = content.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.content = null; // leer
		}
	}

	public byte[] getContent() {
		return content;
	}

	/**
	 * Schreibt die HTTP-Antwort in den angegebenen Strom. Die Ausgabe erfolgt
	 * gemäß RFC 2616.
	 */
	public void writeResponse(OutputStream out) throws ServiceException {
		try {
			StringBuilder header = new StringBuilder();

			int contentLength;
			if (content != null) {
				contentLength = content.length;
			} else {
				contentLength = 0;
			}

			header.append("HTTP/1.1 ");
			header.append(statusCode);
			header.append(' ');
			header.append(statusCode2Reason(statusCode));
			header.append(CRLF);

			header.append("Content-Type: ");
			header.append(contentType);
			header.append(CRLF);

			if (redirectUrl != null) {
				header.append("Location: ");
				header.append(EscapeUtils.encodeUrl(redirectUrl));
				header.append(CRLF);
			}

			header.append("Content-Length: ");
			header.append(contentLength);
			header.append(CRLF);

			if (!cacheable) {
				header.append("Cache-Control: no-cache");
				header.append(CRLF);
				header.append("Pragma: no-cache");
				header.append(CRLF);
			}

			header.append("Connection: close");
			header.append(CRLF);

			// header beenden durch zweiten Zeilenwechsel
			header.append(CRLF);

			// header schreiben
			byte[] headerData = header.toString().getBytes("UTF-8");
			out.write(headerData);

			// body schreiben
			if (content != null) {
				out.write(content);
			}
			out.flush();
		} catch (UnsupportedEncodingException e) {
			throw new ServiceException("Error converting HTTP header into UTF-8", e);
		} catch (IOException e) {
			throw new ServiceException("Error writing HTTP response stream", e);
		}
	}

	/**
	 * Gibt zum Statuscode den zugehörigen Text aus. Entspricht HTTP/1.1.
	 */
	private static String statusCode2Reason(int statusCode) {
		switch (statusCode) {
		case 100:
			return "Continue";
		case 101:
			return "Switching Protocols";

		case 200:
			return "OK";
		case 201:
			return "Created";
		case 202:
			return "Accepted";
		case 203:
			return "Non-Authoritative Information";
		case 204:
			return "No Content";
		case 205:
			return "Reset Content";
		case 206:
			return "Partial Content";

		case 300:
			return "Multiple Choices";
		case 301:
			return "Moved Permanently";
		case 302:
			return "Moved Temporarily";
		case 303:
			return "See Other";
		case 304:
			return "Not Modified";
		case 305:
			return "Use Proxy";
		case 307:
			return "Temporary Redirect";

		case 400:
			return "Bad Request";
		case 401:
			return "Unauthorized";
		case 402:
			return "Payment Required";
		case 403:
			return "Forbidden";
		case 404:
			return "Not Found";
		case 405:
			return "Method Not Allowed";
		case 406:
			return "Not Acceptable";
		case 407:
			return "Proxy Authentication Required";
		case 408:
			return "Request Time-out";
		case 409:
			return "Conflict";
		case 410:
			return "Gone";
		case 411:
			return "Length Required";
		case 412:
			return "Precondition Failed";
		case 413:
			return "Request Entity Too Large";
		case 414:
			return "Request-URI Too Large";
		case 415:
			return "Unsupported Media Type";
		case 416:
			return "Requested range not satisfiable";
		case 417:
			return "Expectation Failed";

		case 500:
			return "Internal Server Error";
		case 501:
			return "Not Implemented";
		case 502:
			return "Bad Gateway";
		case 503:
			return "Service Unavailable";
		case 504:
			return "Gateway Time-out";
		case 505:
			return "HTTP Version not supported";

		default:
			return "";
		}
	}
}
