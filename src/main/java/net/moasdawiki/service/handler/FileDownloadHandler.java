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

package net.moasdawiki.service.handler;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Bearbeitet Anfragen zum Download von statischen Dateien. Anhand der
 * Dateiendung wird automatisch ein passender Content-Type ermittelt. Diese
 * Klasse wird bei den URLs <tt>/*.*</tt>, <tt>/img/...</tt> und
 * <tt>/file/...</tt> aufgerufen.<br>
 * <br>
 * Mit <tt>/...</tt> werden Dateien im Wurzelordner ("root") des Repositories
 * heruntergeladen.<br>
 * <br>
 * Mit <tt>/img/...</tt> können Grafiken im Repository heruntergeladen werden.
 * In der Regel befinden sich diese im selben Ordner wie die referenzierende
 * Wikiseite.<br>
 * <br>
 * Mit <tt>/file/...</tt> können beliebige Dateien aus dem Repository zum
 * Download angeboten werden.
 */
public class FileDownloadHandler {

	private static final String FILE_NOT_FOUND_KEY = "FileDownloadHandler.fileNotFound";
	private static final String REVERSE_NAVIGATION_KEY = "FileDownloadHandler.reverseNavigation";

	private final Logger logger;
	private final Settings settings;
	private final RepositoryService repositoryService;
	private final HtmlService htmlService;

	/**
	 * Constructor.
	 */
	public FileDownloadHandler(@NotNull Logger logger, @NotNull Settings settings,
							   @NotNull RepositoryService repositoryService, @NotNull HtmlService htmlService) {
		this.logger = logger;
		this.settings = settings;
		this.repositoryService = repositoryService;
		this.htmlService = htmlService;
	}

	/**
	 * Handle file downloads for URL paths starting with "/img/".
	 */
	@NotNull
	public HttpResponse handleDownloadImg(@NotNull String urlPath) {
		String absolutePath = urlPath.substring(4); // cut off "/img"
		return generateFileResponse(absolutePath);
	}

	/**
	 * Handle file downloads for URL paths starting with "/file/".
	 */
	@NotNull
	public HttpResponse handleDownloadFile(@NotNull String urlPath) {
		String absolutePath = urlPath.substring(5); // cut off "/file"
		return generateFileResponse(absolutePath);
	}

	/**
	 * Handle file downloads for the URL paths starting with "/..." (no second "/").
	 *
	 * Example: /favicon.ico
	 */
	@NotNull
	public HttpResponse handleDownloadRoot(@NotNull String urlPath) {
		String rootPath = PathUtils.makeWebPathAbsolute(null, settings.getRootPath());
		String absolutePath = PathUtils.concatWebPaths(rootPath, urlPath);
		return generateFileResponse(absolutePath);
	}

	/**
	 * Liest die angegebene Datei aus dem Dateisystem und generiert einen
	 * Antwortstrom. Wenn die Datei nicht existiert, wird eine Fehlermeldung
	 * erzeugt.
	 * 
	 * @param filePath Dateipfad relativ zum Basisordner. Darf keine
	 *        Rückwärtsnavigation ".." enthalten.
	 */
	@NotNull
	private HttpResponse generateFileResponse(@NotNull String filePath) {
		// auf Rückwärtsnavigation prüfen
		if (filePath.contains("..")) {
			logger.write("File path '" + filePath + "' contains invalid reverse navigation, sending response 403 access denied.");
			return htmlService.generateErrorPage(403, REVERSE_NAVIGATION_KEY, filePath);
		}

		// Datei einlesen
		byte[] fileContent;
		try {
			AnyFile anyFile = new AnyFile(filePath);
			fileContent = repositoryService.readBinaryFile(anyFile);
		} catch (ServiceException e) {
			logger.write("File '" + filePath + "' not found, sending response 404", e);
			return htmlService.generateErrorPage(404, FILE_NOT_FOUND_KEY, filePath);
		}

		// Datei in Antwort verpacken
		HttpResponse response = new HttpResponse();
		response.contentType = getContentType(filePath);
		response.content = fileContent;
		return response;
	}

	/**
	 * Bestimmt den Dateityp anhand der Dateiendung.
	 */
	@NotNull
	private String getContentType(@NotNull String filename) {
		if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			return "image/jpeg";
		} else if (filename.endsWith(".gif")) {
			return "image/gif";
		} else if (filename.endsWith(".png")) {
			return "image/png";
		} else if (filename.endsWith(".ico")) {
			return "image/x-icon";
		} else if (filename.endsWith(".zip")) {
			return "application/zip";
		} else if (filename.endsWith(".pdf")) {
			return "application/pdf";
		} else if (filename.endsWith(".htm") || filename.endsWith(".html")) {
			return "text/html";
		} else if (filename.endsWith(".css")) {
			return "text/css";
		} else if (filename.endsWith(".txt")) {
			return "text/plain";
		} else {
			return "application/octet-stream"; // Binärformat
		}
	}
}
