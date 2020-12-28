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

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.render.HtmlWriter;
import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Öffnet einen Webserver und nimmt HTTP-Anfragen entgegen. Der Port wird aus
 * den Einstellungen geholt.
 * 
 * Obwohl der Server single-threaded ist, wird zum Annehmen von HTTP-Requests
 * ein Thread-Pool verwendet, weil z.B. der Firefox mehrere parallele Requests
 * schickt, wovon einige "speculative" sind, d.h. zur Beschleunigung
 * nachfolgender Requests, die u.U. gar nicht benötigt werden. Diese speculative
 * Requests würden sonst die Request-Verarbeitung blockieren, bis es zum
 * jeweiligen Timeout kommt. Siehe dazu
 * https://bugzilla.mozilla.org/show_bug.cgi?id=853423.
 */
public class Webserver {

	/**
	 * Maximale Wartezeit beim Einlesen der HTTP-Header. Der Firefox verwendet 6
	 * Sekunden bei seinen "speculative requests", daher sollten es mehr sein.
	 */
	private static final int REQUEST_READ_TIMEOUT = 10000; // 10 Sekunden
	private static final String CRLF = "\r\n";

	private final Logger log;
	private final Settings settings;
	private final HtmlService htmlService;
	private final RequestDispatcher requestDispatcher;

	private boolean shutdownRequestAllowed;
	private boolean shutdownRequested;
	private ServerSocket server; // wird von run() gesetzt
	private ExecutorService threadPool; // wird von run() gesetzt
	private final Object synchronizationLock = new Object();

	public Webserver(@NotNull Logger log, @NotNull Settings settings,
					 @NotNull HtmlService htmlService, @NotNull RequestDispatcher requestDispatcher) {
		super();
		this.log = log;
		this.settings = settings;
		this.htmlService = htmlService;
		this.requestDispatcher = requestDispatcher;
	}

	/**
	 * Setzt das Flag, ob der Server vom Benutzer per HTTP-Request
	 * heruntergefahren werden darf. Standardwert ist <code>false</code>.
	 */
	public void setShutdownRequestAllowed(boolean shutdownRequestAllowed) {
		this.shutdownRequestAllowed = shutdownRequestAllowed;
	}

	/**
	 * Startet den Server. Der Aufruf blockiert, d.h. er kehrt erst zurück, wenn
	 * der Server beendet wird.
	 * 
	 * Der Server kann durch Aufruf von stop() von außen beendet werden.
	 */
	public void run() {
		// Server starten
		int port = settings.getServerPort();
		try (ServerSocket server = new ServerSocket(port)) {
			this.server = server; // wird von stop() benötigt
			log.write("Wiki server listening on port " + server.getLocalPort());

			// Aufträge abarbeiten
			threadPool = Executors.newFixedThreadPool(5);
			while (!shutdownRequested) {
				// auf Anfrage eines Clients warten und Verbindung aufnehmen
				final Socket client = server.accept();
				threadPool.execute(() -> handleConnection(client));
			}
			threadPool.shutdown();
		} catch (IOException e) {
			log.write("Error running server on port " + settings.getServerPort(), e);
		}
		log.write("Server stopped");
	}

	/**
	 * Beendet den Server. Muss beim Herunterfahren aufgerufen werden, egal ob
	 * das Herunterfahren von außen oder vom Wikiserver selbst initiiert wurde.
	 */
	public void stop() {
		log.write("Server received stop signal");

		// blockierenden accept()-Aufruf abbrechen
		try {
			if (server != null) {
				server.close();
			}
		} catch (IOException e) {
			log.write("Error closing server", e);
		}

		if (threadPool != null) {
			threadPool.shutdown();
		}
	}

	private void handleConnection(Socket client) {
		long ts1 = System.currentTimeMillis();
		try {
			HttpResponse response = null;
			try {
				// wichtig, damit der Request nicht ewig blockieren kann
				client.setSoTimeout(REQUEST_READ_TIMEOUT);

				// Anfragedaten einlesen
				HttpRequest httpRequest = HttpRequestParser.parse(client.getInputStream());
				httpRequest.clientIP = client.getInetAddress();
				log.write("Incoming request from " + client.getRemoteSocketAddress() + ": " + httpRequest.method + " " + httpRequest.urlPath);

				// Antwort generieren
				synchronized (synchronizationLock) {
					// ab hier nur noch single-threaded
					response = generateResponse(httpRequest, client);
				}
			} catch (Exception e) {
				// bei geschlossener Verbindung nichts loggen --> speculative
				// requests
				if (!client.isClosed()) {
					long ts2 = System.currentTimeMillis();
					log.write("Error handling a request after " + (ts2 - ts1) + " ms", e);
					response = htmlService.generateErrorPage(500, e, "wiki.server.handler.error");
				}
			}

			try {
				// Antwort an Client zurücksenden
				if (response != null && !client.isClosed() && !client.isOutputShutdown()) {
					writeResponse(response, client.getOutputStream());
				}
			} catch (Exception e) {
				log.write("Error sending the response of a request", e);
			}
		} finally {
			try {
				// Verbindung zum Client schließen
				client.close();
			} catch (IOException e) {
				log.write("Error closing socket connection", e);
			}
		}
	}

	/**
	 * Sendet eine Antwort auf die angegebene Anfrage.
	 */
	private HttpResponse generateResponse(HttpRequest httpRequest, Socket client) {
		// ggf. nur Zugriff von localhost erlauben
		if (settings.isOnlyLocalhostAccess() && !client.getInetAddress().isLoopbackAddress()) {
			log.write("Remote access from " + client.getInetAddress().getHostAddress() + " denied");
			return htmlService.generateErrorPage(403, "wiki.server.onlylocalhost");
		}

		// Shutdown-Befehl bearbeiten
		if ("/shutdown".equals(httpRequest.urlPath)) {
			if (shutdownRequestAllowed) {
				shutdownRequested = true;
				return htmlService.generateMessagePage("wiki.server.shutdown.finished");
			} else {
				return htmlService.generateErrorPage(403, "wiki.server.shutdown.denied");
			}
		}

		// Status ausgeben
		if ("/status".equals(httpRequest.urlPath)) {
			return generateStatusPage();
		}

		// dispatch request to corresponding service
		return requestDispatcher.handleRequest(httpRequest);
	}

	private HttpResponse generateStatusPage() {
		HtmlWriter writer;
		writer = new HtmlWriter();
		writer.setTitle("Server Status");

		writer.htmlText("<h1>Server Status</h1>");
		writer.setContinueInNewLine();

		writer.htmlText("<h2>MoasdaWiki Settings</h2>");
		writer.setContinueInNewLine();
		writer.openTag("ul");
		writer.openTag("li");
		writer.htmlText("<b>programname</b> = " + EscapeUtils.escapeHtml(settings.getProgramNameVersion()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>port</b> = " + settings.getServerPort());
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>repository.root</b> = " + EscapeUtils.escapeHtml(settings.getRootPath()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>page.startpage</b> = " + EscapeUtils.escapeHtml(settings.getStartpagePath()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>page.navigation</b> = " + EscapeUtils.escapeHtml(settings.getNavigationPagePath()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>page.html.header</b> = " + EscapeUtils.escapeHtml(settings.getHtmlHeaderPagePath()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>page.header</b> = " + EscapeUtils.escapeHtml(settings.getHeaderPagePath()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>page.footer</b> = " + EscapeUtils.escapeHtml(settings.getFooterPagePath()));
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>page.templates</b> = " + EscapeUtils.escapeHtml(settings.getTemplatesPagePath()));
		writer.closeTag();
		writer.openTag("li");
		writer.htmlText("<b>authentication.onlylocalhost</b> = " + settings.isOnlyLocalhostAccess());
		writer.closeTag();
		writer.setContinueInNewLine();
		writer.openTag("li");
		writer.htmlText("<b>actualtime</b> = " + settings.getActualTime());
		writer.closeTag();
		writer.setContinueInNewLine();

		writer.closeTag(); // ul
		writer.setContinueInNewLine();

		writer.htmlText("<h2>JVM Settings</h2>");
		writer.setContinueInNewLine();
		writer.openTag("ul");
		ArrayList<String> keyList = new ArrayList<>();
		for (Enumeration<Object> keyEnum = System.getProperties().keys(); keyEnum.hasMoreElements();) {
			keyList.add((String) keyEnum.nextElement());
		}
		Collections.sort(keyList);
		for (String key : keyList) {
			writer.openTag("li");
			writer.htmlText("<b>" + key + "</b> = " + EscapeUtils.escapeHtml(System.getProperty(key)));
			writer.closeTag(); // li
			writer.setContinueInNewLine();
		}
		writer.closeTag(); // ul

		return htmlService.convertHtml(writer);
	}

	/**
	 * Schreibt die HTTP-Antwort in den angegebenen Strom. Die Ausgabe erfolgt
	 * gemäß RFC 2616.
	 */
	@SuppressWarnings("CharsetObjectCanBeUsed")
	private void writeResponse(@NotNull HttpResponse httpResponse, @NotNull OutputStream out) throws ServiceException {
		try {
			StringBuilder header = new StringBuilder();

			int contentLength;
			if (httpResponse.content != null) {
				contentLength = httpResponse.content.length;
			} else {
				contentLength = 0;
			}

			header.append("HTTP/1.1 ");
			header.append(httpResponse.statusCode);
			header.append(' ');
			header.append(statusCode2Reason(httpResponse.statusCode));
			header.append(CRLF);

			header.append("Content-Type: ");
			header.append(httpResponse.contentType);
			header.append(CRLF);

			if (httpResponse.redirectUrl != null) {
				header.append("Location: ");
				header.append(EscapeUtils.encodeUrl(httpResponse.redirectUrl));
				header.append(CRLF);
			}

			header.append("Content-Length: ");
			header.append(contentLength);
			header.append(CRLF);

			header.append("Cache-Control: no-cache");
			header.append(CRLF);
			header.append("Pragma: no-cache");
			header.append(CRLF);

			header.append("Connection: close");
			header.append(CRLF);

			// header beenden durch zweiten Zeilenwechsel
			header.append(CRLF);

			// header schreiben
			byte[] headerData = header.toString().getBytes("UTF-8");
			out.write(headerData);

			// body schreiben
			if (httpResponse.content != null) {
				out.write(httpResponse.content);
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
