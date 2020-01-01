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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Settings;
import net.moasdawiki.plugin.Plugin;
import net.moasdawiki.plugin.PluginService;
import net.moasdawiki.plugin.ServiceLocator;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.render.HtmlWriter;
import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.Nullable;

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
 * 
 * @author Herbert Reiter
 */
public class Webserver {

	/**
	 * Maximale Wartezeit beim Einlesen der HTTP-Header. Der Firefox verwendet 6
	 * Sekunden bei seinen "speculative requests", daher sollten es mehr sein.
	 */
	private static final int REQUEST_READ_TIMEOUT = 10000; // 10 Sekunden

	private final Settings settings;
	private final Logger log;
	private final HtmlService htmlService;
	private final PluginService pluginService;

	private boolean shutdownRequestAllowed;
	private boolean shutdownRequested;
	private ServerSocket server; // wird von run() gesetzt
	private ExecutorService threadPool; // wird von run() gesetzt
	private final Object synchronizationLock = new Object();

	public Webserver(ServiceLocator serviceLocator) {
		super();
		this.settings = serviceLocator.getSettings();
		this.log = serviceLocator.getLogger();
		this.htmlService = serviceLocator.getHtmlService();
		this.pluginService = serviceLocator.getPluginService();
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

	/**
	 * Gibt den vom Webserver verwendeten Port zurück. <code>null</code> -->
	 * Webserver läuft nicht.
	 */
	@Nullable
	public Integer getPort() {
		if (server != null) {
			return server.getLocalPort();
		} else {
			return null;
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
					response.writeResponse(client.getOutputStream());
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

		// per URL-Mapping das zuständige Plugin aufrufen
		Plugin plugin = pluginService.getPluginByUrl(httpRequest.urlPath);
		if (plugin != null) {
			HttpResponse response = plugin.handleRequest(httpRequest);
			if (response != null) {
				return response;
			}
			return htmlService.generateErrorPage(404, "wiki.plugin.handleRequest.notsupported", plugin.getClass().getName());
		}

		// unbekannte URL
		return htmlService.generateErrorPage(404, "wiki.server.url.unmapped", httpRequest.urlPath);
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
		writer.htmlText("<b>repository.pagesuffix</b> = " + EscapeUtils.escapeHtml(settings.getPageSuffix()));
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
}
