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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Enthält die geparsten Daten eines HTTP-Requests.
 * 
 * @author Herbert Reiter
 */
public class HttpRequest {

	public static final String HTTP_HEADER_FIRST_LINE = "";
	public static final String HTTP_HEADER_CONTENT_TYPE = "content-type";
	public static final String HTTP_HEADER_CONTENT_LENGTH = "content-length";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";

	/**
	 * IP-Adresse des HTTP-Clients. Nicht <code>null</code>.
	 */
	public InetAddress clientIP;

	/**
	 * Headereinträge im HTTP-Request. Die erste Zeile wird per Leerstring als
	 * Schlüssel aufgenommen. Da die Namen gemäß RFC 2616 case-insensitive sind,
	 * werden sie stets in Kleinbuchstaben abgelegt. Nicht <code>null</code>.
	 */
	public Map<String, String> httpHeader;

	/**
	 * Request-Methode, z.B. <tt>GET</tt> oder <tt>POST</tt>. Nicht
	 * <code>null</code>.
	 */
	public String method;

	/**
	 * Komplette URL inkl. Pfad und Parameter. Nicht <code>null</code>.<br>
	 * <br>
	 * Beispiel: <tt>/pfad/action?param1=value1&amp;param2=value2</tt>
	 */
	public String url;

	/**
	 * Pfadteil der URL, ohne "?" und nachfolgende Parameter. Nicht
	 * <code>null</code>.<br>
	 * <br>
	 * Beispiel: <tt>/pfad/action</tt>
	 */
	public String urlPath;

	/**
	 * Inhalt aller im Request enthaltenen URL-Parameter bzw.
	 * HTML-Formularfelder. Gleichnamige Formularfelder im POST-Abschnitt
	 * überschreiben Formularfelder im GET-String. Nicht <code>null</code>.
	 */
	public Map<String, String> urlParameters;

	/**
	 * HTTP-Body in Rohform. Enthält bei einem HTTP-POST-Request die
	 * HTML-Formulardaten, bei einem Dateiupload die Binärdaten der Datei. Nicht
	 * <code>null</code>.
	 */
	public byte[] httpBody;

	/**
	 * Konstruktor.
	 */
	public HttpRequest() {
		httpHeader = new HashMap<>();
		method = "";
		url = "";
		urlPath = "";
		urlParameters = new HashMap<>();
		httpBody = new byte[0];
	}
}
