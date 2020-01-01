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

package net.moasdawiki.service.render;

import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.util.JavaScriptUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hilfsmethoden zum Generieren von JSON-HTTP-Responses.
 * 
 * @author Herbert Reiter
 */
public abstract class JsonHelper {

	/**
	 * Generiert eine HTTP-Antwort mit angegebenem Statuscode und einem
	 * JSON-Objekt mit einem "message"-Eintrag. Wird u.a. für
	 * Ajax-Fehlerantworten verwendet.
	 * 
	 * @param statusCode HTTP-Statuscode.
	 * @param message Nachricht.
	 */
	public static HttpResponse generateResponse(int statusCode, @NotNull String message) {
		HttpResponse result = new HttpResponse();
		result.setStatusCode(statusCode);
		result.setContentType(HttpResponse.CONTENT_TYPE_JSON_UTF8);

		String jsonText = "{'message': '" + JavaScriptUtils.escapeJavaScript(message) + "'}";
		result.setContent(jsonText);
		return result;
	}

	/**
	 * Generiert eine JSON-Antwort mit Statuscode. Als HTTP-Statuscode wird
	 * stets 200 verwendet.
	 * 
	 * @param code Ergebniscode; 0 --> OK, andere Codes haben eine individuelle
	 *        Bedeutung.
	 */
	@NotNull
	public static HttpResponse generateJsonResponse(int code) {
		return generateJsonResponse(code, null);
	}

	/**
	 * Generiert eine JSON-Antwort mit Statuscode und einer Nachricht. Als
	 * HTTP-Statuscode wird stets 200 verwendet.
	 * 
	 * @param code Ergebniscode; 0 --> OK, 1 --> allgemeiner Fehler, andere
	 *        Codes haben eine individuelle Bedeutung.
	 * @param message Nachricht, <code>null</code> --> keine Nachricht.
	 */
	@NotNull
	public static HttpResponse generateJsonResponse(int code, @Nullable String message) {
		HttpResponse result = new HttpResponse();
		result.setContentType(HttpResponse.CONTENT_TYPE_JSON_UTF8);

		String jsonText = "{ 'code': " + code;
		if (message != null) {
			jsonText += ", 'message': '" + JavaScriptUtils.escapeJavaScript(message) + "'";
		}
		jsonText += " }";
		result.setContent(jsonText);
		return result;
	}
}
