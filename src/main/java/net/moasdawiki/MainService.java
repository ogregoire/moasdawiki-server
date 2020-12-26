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

package net.moasdawiki;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.Settings;
import net.moasdawiki.server.RequestDispatcher;
import net.moasdawiki.server.Webserver;
import net.moasdawiki.service.handler.EditorHandler;
import net.moasdawiki.service.handler.FileDownloadHandler;
import net.moasdawiki.service.handler.SearchHandler;
import net.moasdawiki.service.handler.ViewPageHandler;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.search.SearchService;
import net.moasdawiki.service.sync.SynchronizationService;
import net.moasdawiki.service.transform.*;
import net.moasdawiki.service.wiki.WikiService;

import java.io.File;

/**
 * Main control of the wiki server.
 *
 * Will be called by the Apache Commons Daemon
 * (http://jakarta.apache.org/commons/daemon/) and also by the Main class.
 */
public class MainService {
	private static final String REPOSITORY_ROOT_PATH_DEFAULT = "repository";

	private Settings settings;
	private Messages messages;
	private Webserver webserver;

	/**
	 * Load settings and initialize the server.
	 */
	public void init(String[] args) {
		// initialize layers
		Logger logger = new Logger(System.out);
		logger.write("MoasdaWiki starting");

		// determine the repository base folder
		File repositoryRoot;
		if (args != null && args.length >= 1) {
			repositoryRoot = new File(args[0]);
		} else {
			repositoryRoot = new File(REPOSITORY_ROOT_PATH_DEFAULT);
		}

		// basic services
		RepositoryService repositoryService = new RepositoryService(logger, repositoryRoot);
		repositoryService.init();
		settings = new Settings(logger, repositoryService, Settings.getConfigFileServer());
		messages = new Messages(logger, settings, repositoryService);
		WikiService wikiService = new WikiService(logger, repositoryService);
		SearchService searchService = new SearchService(logger, repositoryService, wikiService, false);
		SynchronizationService synchronizationService = new SynchronizationService(logger, settings, repositoryService);

		// transformers
		IncludePageTransformer includePageTransformer = new IncludePageTransformer(logger, wikiService);
		KontaktseiteTransformer kontaktseiteTransformer = new KontaktseiteTransformer();
		TerminTransformer terminTransformer = new TerminTransformer(logger, messages, repositoryService, wikiService);
		SynchronizationPageTransformer synchronizationPageTransformer = new SynchronizationPageTransformer(messages, synchronizationService);
		WikiTagsTransformer wikiTagsTransformer = new WikiTagsTransformer(logger, settings, messages, wikiService);
		// list of transformers, the order matters
		TransformWikiPage[] transformers = {includePageTransformer, kontaktseiteTransformer, terminTransformer, synchronizationPageTransformer, wikiTagsTransformer};
		TransformerService transformerService = new TransformerService(transformers);

		// more services
		HtmlService htmlService = new HtmlService(logger, settings, messages, wikiService, transformerService);

		// HTTP handlers
		ViewPageHandler viewPageHandler = new ViewPageHandler(logger, settings, wikiService, htmlService);
		SearchHandler searchHandler = new SearchHandler(logger, settings, messages, wikiService, searchService, htmlService);
		EditorHandler editorHandler = new EditorHandler(logger, settings, messages, repositoryService, wikiService, transformerService, htmlService);
		FileDownloadHandler fileDownloadHandler = new FileDownloadHandler(logger, settings, repositoryService, htmlService);

		// web server
		RequestDispatcher requestDispatcher = new RequestDispatcher(htmlService, viewPageHandler,
				searchHandler, editorHandler, fileDownloadHandler, synchronizationService);
		webserver = new Webserver(logger, settings, htmlService, requestDispatcher);
	}

	/**
	 * Set flag if the server can be shut down by the user.
	 */
	public void setShutdownRequestAllowed(boolean shutdownRequestAllowed) {
		if (webserver != null) {
			webserver.setShutdownRequestAllowed(shutdownRequestAllowed);
		}
	}

	public Settings getSettings() {
		return settings;
	}

	public Messages getMessages() {
		return messages;
	}

	/**
	 * Starts the web server. This method blocks until the server stops.
	 *
	 * To stop the server from outside call the {@link #stop()} method.
	 */
	public void runBlocking() {
		webserver.run();
	}

	/**
	 * Starts the web server. This method runs asynchronously and returns
	 * immediately (non-blocking).
	 */
	public void start() {
		// run blocking call in a separate thread
		Thread t = new Thread("WikiServer") {
			public void run() {
				runBlocking();
			}
		};
		t.start();
	}

	/**
	 * Stops the web server.
	 */
	public void stop() {
		webserver.stop();
	}

	/**
	 * Free memory. Method is required by Apache Commons Daemon.
	 */
	@SuppressWarnings({"unused", "EmptyMethod"})
	public void destroy() {
		// noop
	}
}
