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

package net.moasdawiki.service.sync;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.EscapeUtils;
import net.moasdawiki.util.JavaScriptUtils;
import net.moasdawiki.util.StringUtils;
import net.moasdawiki.util.xml.XmlGenerator;
import net.moasdawiki.util.xml.XmlParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * Stellt einen Webservice zum Synchronisieren des lokalen Repositories mit
 * einem Client-Repository (z.B. App) bereit. Als Referenz-Uhrzeit wird stets
 * die des Servers verwendet, um unerwünschte Effekte durch asynchrone
 * Systemuhren zu vermeiden.
 * 
 * Bezüglich der Aufruf-URLs und Datensyntax siehe die Doku der jeweiligen
 * Methoden in dieser Klasse.
 */
public class SynchronizationService {

	/**
	 * Pfad der Cachedatei mit der Liste aller Repository-Dateien.
	 */
	public static final String SESSION_LIST_FILEPATH = "/syncsessions.config";

	private static final String PROTOCOL_VERSION = "2.0";

	/**
	 * Maximale Dateigröße beim Download beschränken, weil sonst die App mit
	 * einer OutOfMemoryException abstürzt.
	 */
	private static final int MAX_READ_FILE_SIZE = 10 * 1000 * 1000; // 10 MB

	/**
	 * File paths to be excluded from synchronization.
	 */
	private static final String[] EXLUDE_FILEPATHS = { SESSION_LIST_FILEPATH };

	private final Logger logger;
	private final Settings settings;
	private final RepositoryService repositoryService;
	private final SecureRandom random = new SecureRandom();

	/**
	 * Map: Session ID --> session data.
	 */
	private final Map<String, SessionData> sessionMap;

	/**
	 * Constructor.
	 */
	public SynchronizationService(@NotNull Logger logger, @NotNull Settings settings,
								  @NotNull RepositoryService repositoryService) {
		this.logger = logger;
		this.settings = settings;
		this.repositoryService = repositoryService;
		this.sessionMap = new HashMap<>();
		readSessionList();
	}

	/**
	 * Returns the session list.
	 */
	@NotNull
	public List<SessionData> getSessions() {
		return new ArrayList<>(sessionMap.values());
	}

	/**
	 * Erteilt einer Session die Berechtigung, die URLs zum Synchronisieren von
	 * Dateien aufzurufen. Wird von der GUI aufgerufen.<br>
	 * <br>
	 * URL-Pfad: <code>/sync-gui/session-permit</code> (GET)<br>
	 * <br>
	 * URL-Parameter:
	 * <ul>
	 * <li><code>session-id</code>: Session-ID. Nicht <code>null</code>.</li>
	 * </ul>
	 *
	 * JSON-Antwort:
	 * 
	 * <pre>
	 * {
	 *   'code': 0 = ok / 1 = Fehler,
	 *   'message': Fehlertext
	 * }
	 * </pre>
	 */
	@NotNull
	public HttpResponse handleSessionPermit(@Nullable String sessionId) {
		// Session-ID prüfen
		if (sessionId == null) {
			return generateJsonResponse(1, "Parameter session-id missing");
		}

		// Session genehmigen
		SessionData sessionData = sessionMap.get(sessionId);
		if (sessionData == null) {
			return generateJsonResponse(1, "Session unknown: " + sessionId);
		}
		sessionData.authorized = true;
		writeSessionList();
		return generateJsonResponse(0);
	}

	/**
	 * Löscht eine Session aus der Sessionliste. Damit verliert sie automatisch
	 * auch eine evtl. erteilte Zugangsberechtigung zum Synchronisieren von
	 * Dateien. Wird von der GUI aufgerufen.<br>
	 * <br>
	 * URL-Pfad: <code>/sync-gui/session-drop</code> (GET)<br>
	 * <br>
	 * URL-Parameter:
	 * <ul>
	 * <li><code>session-id</code>: Session-ID. Nicht <code>null</code>.</li>
	 * </ul>
	 *
	 * JSON-Antwort:
	 * 
	 * <pre>
	 * {
	 *   'code': 0 = ok / 1 = Fehler,
	 *   'message': Fehlertext
	 * }
	 * </pre>
	 */
	@NotNull
	public HttpResponse handleSessionDrop(@Nullable String sessionId) {
		// Session-ID prüfen
		if (sessionId == null) {
			return generateJsonResponse(1, "Parameter session-id missing");
		}

		// Session löschen
		SessionData sessionData = sessionMap.remove(sessionId);
		if (sessionData == null) {
			return generateJsonResponse(1, "Session unknown: " + sessionId);
		}
		writeSessionList();
		return generateJsonResponse(0);
	}

	@SuppressWarnings("SameParameterValue")
	private HttpResponse generateJsonResponse(int code) {
		return generateJsonResponse(code, null);
	}

	private HttpResponse generateJsonResponse(int code, @Nullable String jsonText) {
		HttpResponse result = new HttpResponse();
		result.contentType = HttpResponse.CONTENT_TYPE_JSON_UTF8;
		result.setContent(JavaScriptUtils.generateJson(code, jsonText));
		return result;
	}

	/**
	 * Erzeugt eine Session, über die ein Wiki-Client mit dem Server Daten
	 * austauschen kann. Die Session ist an einen einzelnen Client gebunden und
	 * muss manuell auf dem Server authorisiert werden. Damit wird
	 * sichergestellt, dass sich der Server nicht mit einem fremden Client
	 * synchronisiert und damit unbeabsichtigt seinen gesamten Inhalt preisgibt.
	 * <br>
	 * <br>
	 * Auch der Server identifiziert sich gegenüber dem Client, indem er Angaben
	 * über sich macht und sich eine zufällige Client-Session-ID merken muss,
	 * die er bei späteren Aufrufen von <code>check-session</code> korrekt
	 * wiedergeben muss. Damit wird sichergestellt, dass die späteren
	 * Synchronisierungsaufrufe des Clients an denselben Server gehen und damit
	 * nicht unbeabsichtigt der gesamte Inhalt des Clients an einen fremden
	 * Wikiserver übermittelt werden. Da sich das Mobilgerät mit der App an
	 * viele verschiedene WLAN-Knoten anmeldet, wäre das ein leichtes
	 * Angriffsszenario.<br>
	 * <br>
	 * URL-Pfad: <code>/sync/create-session</code> (POST)<br>
	 * <br>
	 * POST-Daten:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;create-session version="2.0">
	 *   &lt;client-session-id>abcdefghi&lt;/client-session-id>
	 *   &lt;client-name>MoasdaWiki-App&lt;/client-name>
	 *   &lt;client-version>2.1&lt;/client-version>
	 *   &lt;client-host>ANDROIDPHONE1&lt;/client-host>
	 * &lt;/create-session>
	 * </pre>
	 *
	 * HTTP-Antwort:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;create-session-response version="2.0">
	 *   &lt;server-session-id>1234567890&lt;/server-session-id>
	 *   &lt;server-name>MoasdaWiki&lt;/server-name>
	 *   &lt;server-version>2.0&lt;/server-version>
	 *   &lt;server-host>SERVERNAME1&lt;/server-host>
	 * &lt;/create-session-response>
	 * </pre>
	 */
	public HttpResponse handleCreateSession(byte @NotNull [] httpBody) {
		try {
			// XML einlesen
			String requestXml = fromUtf8Bytes(httpBody);
			logger.write("Received XML: " + requestXml);
			CreateSessionXml createSession = parseXml(requestXml, CreateSessionXml.class);

			// Session erzeugen
			SessionData sessionData = new SessionData();
			sessionData.serverSessionId = generateSessionId();
			sessionData.clientSessionId = createSession.clientSessionId;
			sessionData.createTimestamp = new Date();
			sessionData.clientName = createSession.clientName;
			sessionData.clientVersion = createSession.clientVersion;
			sessionData.clientHost = createSession.clientHost;
			sessionMap.put(sessionData.serverSessionId, sessionData);
			writeSessionList();

			// Antwort-XML senden
			CreateSessionResponseXml createSessionResponseXml = new CreateSessionResponseXml();
			createSessionResponseXml.version = PROTOCOL_VERSION;
			createSessionResponseXml.serverSessionId = sessionData.serverSessionId;
			createSessionResponseXml.serverName = settings.getProgramName();
			createSessionResponseXml.serverVersion = settings.getVersion();
			createSessionResponseXml.serverHost = settings.getServerHost();
			String responseXml = generateXml(createSessionResponseXml);
			logger.write("Sending XML: " + responseXml);

			HttpResponse response = new HttpResponse();
			response.contentType = HttpResponse.CONTENT_TYPE_XML;
			response.setContent(responseXml);
			return response;
		} catch (ServiceException e) {
			String msg = "Error generating synchronization session";
			logger.write(msg, e);
			return generateErrorResponse(msg);
		}
	}

	private String generateSessionId() {
		return new BigInteger(130, random).toString(32);
	}

	/**
	 * Überprüft die angegebene Server-Session-ID und gibt zurück, ob die
	 * Session für die Synchronisierung bereits freigeschaltet wurde.<br>
	 * <br>
	 * URL-Pfad: <code>/sync/check-session</code> (POST)<br>
	 * <br>
	 * POST-Daten:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;check-session version="2.0">
	 *   &lt;server-session-id>1234567890&lt;/server-session-id>
	 * &lt;/check-session>
	 * </pre>
	 *
	 * HTTP-Antwort:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;check-session-response version="2.0">
	 *   &lt;valid>true&lt;/valid>
	 *   &lt;authorized>true&lt;/authorized>
	 *   &lt;client-session-id>abcdefghi&lt;/client-session-id>
	 * &lt;/check-session-response>
	 * </pre>
	 */
	public HttpResponse handleCheckSession(byte @NotNull [] httpBody) {
		try {
			// XML einlesen
			String requestXml = fromUtf8Bytes(httpBody);
			logger.write("Received XML: " + requestXml);
			CheckSessionXml checkSession = parseXml(requestXml, CheckSessionXml.class);

			// Session prüfen
			if (checkSession.serverSessionId == null) {
				return generateErrorResponse("Parameter server-session-id missing");
			}
			SessionData sessionData = sessionMap.get(checkSession.serverSessionId);

			// Antwort-XML senden
			CheckSessionResponseXml checkSessionResponseXml = new CheckSessionResponseXml();
			checkSessionResponseXml.version = PROTOCOL_VERSION;
			checkSessionResponseXml.valid = (sessionData != null);
			if (sessionData != null) {
				checkSessionResponseXml.authorized = sessionData.authorized;
				checkSessionResponseXml.clientSessionId = sessionData.clientSessionId;
			}
			String responseXml = generateXml(checkSessionResponseXml);
			logger.write("Sending XML: " + responseXml);

			HttpResponse response = new HttpResponse();
			response.contentType = HttpResponse.CONTENT_TYPE_XML;
			response.setContent(responseXml);
			return response;
		} catch (ServiceException e) {
			String msg = "Error generating synchronization session";
			logger.write(msg, e);
			return generateErrorResponse(msg);
		}
	}

	/**
	 * Listet alle seit der letzten Synchronisierung auf dem Server geänderte
	 * Dateien auf.<br>
	 * <br>
	 * URL-Pfad: <code>/sync/list-modified-files</code> (POST)<br>
	 * <br>
	 * POST-Daten:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;list-modified-files version="2.0">
	 *   &lt;server-session-id>1234567890&lt;/server-session-id>
	 *   &lt;last-sync-server-time>2015-03-01T10:00:00.000Z&lt;/last-sync-server-time>
	 * &lt;/list-modified-files>
	 * </pre>
	 * 
	 * <code>server-session-id</code> enhält die Server-Session-ID, sie muss
	 * autorisiert sein. Nicht <code>null</code>.<br>
	 * <code>last-sync-server-time</code> enthält die Serverzeit (nicht
	 * Clientzeit!) der letzten Synchronisierung. Format
	 * <code>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</code> in UTC, Wenn der Parameter
	 * fehlt, werden alle Dateien aufgelistet, d.h. das Repository wurde noch
	 * nie synchronisiert.<br>
	 * <br>
	 * HTTP-Antwort:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;list-modified-files-response version="2.0" current-server-time="2015-03-01T10:00:00.000Z">
	 *   &lt;file timestamp="2015-02-25T15:30:00.000Z">/Startseite.txt&lt;/file>
	 *   &lt;file timestamp="2015-02-23T11:45:00.000Z">/grafik.png&lt;/file>
	 * &lt;/list-modified-files-response>
	 * </pre>
	 */
	@NotNull
	public HttpResponse handleListModifiedFiles(byte @NotNull [] httpBody) {
		try {
			// XML einlesen
			String requestXml = fromUtf8Bytes(httpBody);
			logger.write("Received XML: " + requestXml);
			ListModifiedFilesXml listModifiedFiles = parseXml(requestXml, ListModifiedFilesXml.class);

			// Session-ID prüfen
			if (listModifiedFiles.serverSessionId == null) {
				return generateErrorResponse("Parameter server-session-id missing");
			}
			SessionData sessionData = sessionMap.get(listModifiedFiles.serverSessionId);
			if (sessionData == null) {
				return generateErrorResponse("Session unknown: " + listModifiedFiles.serverSessionId);
			} else if (!sessionData.authorized) {
				return generateErrorResponse("Session not authorized: " + listModifiedFiles.serverSessionId);
			}
			sessionData.lastSyncTimestamp = new Date();

			// geänderte Dateien abfragen
			Date lastSyncServerTime = parseUtcDateOrNull(listModifiedFiles.lastSyncServerTime);
			Set<AnyFile> files = getModifiedFiles(lastSyncServerTime);

			// XML-Antwort generieren
			ListModifiedFilesResponseXml xmlResponse = new ListModifiedFilesResponseXml();
			xmlResponse.version = PROTOCOL_VERSION;
			xmlResponse.currentServerTime = DateUtils.formatUtcDate(new Date());
			xmlResponse.fileList = new ArrayList<>();
			for (AnyFile file : files) {
				SingleFileXml singleFile = new SingleFileXml();
				singleFile.timestamp = DateUtils.formatUtcDate(file.getContentTimestamp());
				singleFile.filePath = file.getFilePath();
				xmlResponse.fileList.add(singleFile);
			}

			String responseXml = generateXml(xmlResponse);
			logger.write("Sending XML: " + responseXml);
			HttpResponse response = new HttpResponse();
			response.contentType = HttpResponse.CONTENT_TYPE_XML;
			response.setContent(responseXml);
			return response;
		} catch (ServiceException e) {
			String msg = "Error sending modified files list";
			logger.write(msg, e);
			return generateErrorResponse(msg);
		}
	}

	/**
	 * Gibt die seit der letzten Synchroniserung geänderten Dateien zurück. Die
	 * bei der Synchronisierung auszuschließenden Dateien werden automatisch aus
	 * der Liste entfernt.
	 */
	private Set<AnyFile> getModifiedFiles(Date lastSyncServerTime) {
		Set<AnyFile> filesModified = repositoryService.getModifiedAfter(lastSyncServerTime);
		for (String filePath : EXLUDE_FILEPATHS) {
			filesModified.remove(new AnyFile(filePath));
		}
		return filesModified;
	}

	/**
	 * Lädt eine Datei aus dem lokalen Repository herunter.<br>
	 * <br>
	 * URL-Pfad: <code>/sync/read-file</code> (POST)<br>
	 * <br>
	 * POST-Daten:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;read-file version="2.0">
	 *   &lt;server-session-id>1234567890&lt;/server-session-id>
	 *   &lt;file-path>/Startseite.txt&lt;/file-path>
	 * &lt;/read-file>
	 * </pre>
	 * 
	 * <code>server-session-id</code> enhält die Server-Session-ID, sie muss
	 * autorisiert sein. Nicht <code>null</code>.<br>
	 * <code>file-path</code> Dateipfad im Repository, die gelesen werden soll.
	 * Nicht <code>null</code>.<br>
	 * <br>
	 * HTTP-Antwort:
	 * 
	 * <pre>
	 * &lt;?xml version="1.0" encoding="UTF-8"?>
	 * &lt;read-file-response version="2.0">
	 *   &lt;timestamp>2015-02-25T15:30:00.000Z&lt;/timestamp>
	 *   &lt;content>base64-encoded file content...&lt;/content>
	 * &lt;/read-file-response>
	 * </pre>
	 * 
	 * <code>timestamp</code> Zeitstempel der letzten Dateiänderung. Nicht
	 * <code>null</code>.<br>
	 * <code>content</code> Inhalt der Datei in binärer Form, base64-kodiert.
	 * Nicht <code>null</code>.
	 */
	@NotNull
	public HttpResponse handleReadFile(byte @NotNull [] httpBody) {
		try {
			// XML einlesen
			String requestXml = fromUtf8Bytes(httpBody);
			logger.write("Received XML: " + requestXml);
			ReadFileXml readFileXml = parseXml(requestXml, ReadFileXml.class);

			// Session-ID prüfen
			if (readFileXml.serverSessionId == null) {
				return generateErrorResponse("Parameter server-session-id missing");
			}
			SessionData sessionData = sessionMap.get(readFileXml.serverSessionId);
			if (sessionData == null) {
				return generateErrorResponse("Session unknown: " + readFileXml.serverSessionId);
			} else if (!sessionData.authorized) {
				return generateErrorResponse("Session not authorized: " + readFileXml.serverSessionId);
			}

			// Datei einlesen
			AnyFile anyFile = repositoryService.getFile(readFileXml.filePath);
			if (anyFile == null) {
				return generateErrorResponse("File not found: " + readFileXml.filePath);
			}
			byte[] fileContent = repositoryService.readBinaryFile(anyFile);

			// Zu große Dateien lassen die App abstürzen
			if (fileContent.length > MAX_READ_FILE_SIZE) {
				return generateErrorResponse("File size too big for synchronization: " + readFileXml.filePath);
			}

			// Antwort generieren
			ReadFileResponseXml xmlResponse = new ReadFileResponseXml();
			xmlResponse.version = PROTOCOL_VERSION;
			xmlResponse.timestamp = DateUtils.formatUtcDate(anyFile.getContentTimestamp());
			xmlResponse.content = EscapeUtils.encodeBase64(fileContent);
			String responseXml = generateXml(xmlResponse);
			logger.write("Sending XML: " + truncateLogText(responseXml));
			HttpResponse response = new HttpResponse();
			response.contentType = HttpResponse.CONTENT_TYPE_XML;
			response.setContent(responseXml);
			return response;
		} catch (ServiceException e) {
			String msg = "Error reading file content";
			logger.write(msg, e);
			return generateErrorResponse(msg);
		}
	}

	private String truncateLogText(String logText) {
		if (logText.length() <= 200) {
			return logText;
		}
		return logText.substring(0, 200) + '…';
	}

	/**
	 * Erzeugt eine HTTP-Fehlerantwort mit dem angegebenen Fehlertext.
	 * 
	 * @param message Fehlertext. Nicht <code>null</code>.
	 * @return HTTP-Antwort. Nicht <code>null</code>.
	 */
	private HttpResponse generateErrorResponse(String message) {
		try {
			ErrorResponseXml errorResponseXml = new ErrorResponseXml();
			errorResponseXml.version = PROTOCOL_VERSION;
			errorResponseXml.message = message;
			String responseXml = generateXml(errorResponseXml);
			logger.write("Sending error XML: " + responseXml);

			HttpResponse response = new HttpResponse();
			response.contentType = HttpResponse.CONTENT_TYPE_XML;
			response.setContent(responseXml);
			return response;
		} catch (ServiceException e) {
			logger.write("Cannot generate error response XML, sending 500", e);
			HttpResponse response = new HttpResponse();
			response.statusCode = 500; // internal error
			return response;
		}
	}

	@Nullable
	private static Date parseUtcDateOrNull(String dateStr) {
		try {
			return DateUtils.parseUtcDate(dateStr);
		} catch (ServiceException e) {
			return null;
		}
	}

	/**
	 * Wandelt eine JAXB-Bean in einen XML-Strom um.
	 */
	private String generateXml(AbstractSyncXml xmlBean) throws ServiceException {
		XmlGenerator xmlGenerator = new XmlGenerator(logger);
		return xmlGenerator.generate(xmlBean);
	}

	/**
	 * Wandelt einen XML-Strom in eine JAXB-Bean um.
	 */
	private <T extends AbstractSyncXml> T parseXml(String xml, Class<T> xmlBeanType) throws ServiceException {
		XmlParser xmlParser = new XmlParser(logger);
		return xmlParser.parse(xml, xmlBeanType);
	}

	private static String fromUtf8Bytes(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * Lädt eine zuvor gespeicherte Sessionliste.
	 */
	private void readSessionList() {
		AnyFile sessionListFile = repositoryService.getFile(SESSION_LIST_FILEPATH);
		if (sessionListFile == null) {
			// Datei existiert nicht, keinen Stacktrace loggen, nur Notiz
			logger.write("Session list file not found: " + SESSION_LIST_FILEPATH);
			return;
		}

		try {
			String fileContent = repositoryService.readTextFile(sessionListFile);
			BufferedReader reader = new BufferedReader(new StringReader(fileContent));
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				String[] token = line.split("\t", 8);
				if (token.length < 7) {
					// Zeile ignorieren
					continue;
				}

				SessionData sessionData = new SessionData();
				sessionData.serverSessionId = StringUtils.emptyToNull(token[0]);
				sessionData.clientSessionId = StringUtils.emptyToNull(token[1]);
				sessionData.createTimestamp = DateUtils.parseUtcDate(StringUtils.emptyToNull(token[2]));
				sessionData.clientName = StringUtils.emptyToNull(token[3]);
				sessionData.clientVersion = StringUtils.emptyToNull(token[4]);
				sessionData.clientHost = StringUtils.emptyToNull(token[5]);
				sessionData.authorized = Boolean.parseBoolean(token[6]);
				sessionMap.put(sessionData.serverSessionId, sessionData);
				count++;
			}
			reader.close();
			logger.write(count + " sessions read");
		} catch (Exception e) {
			logger.write("Error reading session list file " + SESSION_LIST_FILEPATH, e);
		}
	}

	/**
	 * Speichert die Sessionliste, damit sie nach dem Neustart des Wikiservers
	 * wieder geladen werden kann und die Sessions damit weiterhin gültig sind.
	 */
	private void writeSessionList() {
		StringBuilder sb = new StringBuilder();
		for (String serverSessionId : sessionMap.keySet()) {
			SessionData sessionData = sessionMap.get(serverSessionId);
			sb.append(StringUtils.nullToEmpty(sessionData.serverSessionId));
			sb.append('\t');
			sb.append(StringUtils.nullToEmpty(sessionData.clientSessionId));
			sb.append('\t');
			sb.append(StringUtils.nullToEmpty(DateUtils.formatUtcDate(sessionData.createTimestamp)));
			sb.append('\t');
			sb.append(StringUtils.nullToEmpty(sessionData.clientName));
			sb.append('\t');
			sb.append(StringUtils.nullToEmpty(sessionData.clientVersion));
			sb.append('\t');
			sb.append(StringUtils.nullToEmpty(sessionData.clientHost));
			sb.append('\t');
			sb.append(sessionData.authorized);
			sb.append('\n');
		}
		String fileContent = sb.toString();

		// Datei schreiben
		try {
			AnyFile anyFile = new AnyFile(SESSION_LIST_FILEPATH);
			repositoryService.writeTextFile(anyFile, fileContent);
			logger.write(sessionMap.size() + " sessions written");
		} catch (ServiceException e) {
			logger.write("Error writing session list file " + SESSION_LIST_FILEPATH, e);
		}
	}
}
