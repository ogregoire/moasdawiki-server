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

package net.moasdawiki.service;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;

/**
 * Erzeugt eine HTTP-Antwort. Diese besteht aus einem HTTP-Header und ggf. dem
 * Body-Abschnitt.
 */
@SuppressWarnings("unused")
public class HttpResponse {
	public static final String CONTENT_TYPE_HTML = "text/html";
	public static final String CONTENT_TYPE_XML = "text/xml";
	public static final String CONTENT_TYPE_TEXT = "text/plain";
	public static final String CONTENT_TYPE_BINARY = "application/octet-stream";
	public static final String CONTENT_TYPE_JSON_UTF8 = "application/json; charset=utf-8";

	public int statusCode;
	public String redirectUrl;
	public String contentType;
	public byte[] content; // HTTP-Body in Maschinendarstellung; null = leer

	/**
	 * Konstruktor.
	 */
	public HttpResponse() {
		this.statusCode = 200;
		this.contentType = CONTENT_TYPE_HTML;
	}

	/**
	 * Setzt eine Redirect-URL, die in den HTTP-Header geschrieben wird.
	 * 
	 * @param redirectUrl URL, an die weitergeleitet werden soll.
	 */
	public void setRedirectUrl(@NotNull String redirectUrl) {
		statusCode = 302;
		this.redirectUrl = redirectUrl;
	}

	/**
	 * Übernimmt einen Text im UTF-8-Format in den Ausgabestrom.
	 */
	@SuppressWarnings("CharsetObjectCanBeUsed")
	public void setContent(@NotNull String content) {
		try {
			this.content = content.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.content = null; // leer
		}
	}
}
