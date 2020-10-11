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
import net.moasdawiki.plugin.PluginService;
import net.moasdawiki.plugin.ServiceLocator;
import net.moasdawiki.server.Webserver;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.repository.FilesystemRepositoryService;
import net.moasdawiki.service.search.SearchService;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.WikiServiceImpl;

import java.io.File;

/**
 * Hauptschnittstelle für den Aufruf des Wiki-Servers. Wird beim Starten mittels
 * Apache Commons Daemon (http://jakarta.apache.org/commons/daemon/) sowie über
 * Kommandozeile verwendet.
 * 
 * @author Herbert Reiter
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class MainService {
	private static final String REPOSITORY_ROOT_PATH_DEFAULT = "repository";

	private Logger logger;
	private Settings settings;
	private Messages messages;
	private FilesystemRepositoryService repositoryService;
	private WikiService wikiService;
	private SearchService searchService;
	private PluginService pluginService;
	private HtmlService htmlService;
	private ServiceLocator serviceLocator;
	private Webserver webserver;

	/**
	 * Einstellungen laden und Server initialiseren.
	 */
	public void init(String[] args) {
		// Schichten initialisieren
		logger = new Logger(System.out);
		logger.write("MoasdaWiki starting");

		// Repository-Basisordner bestimmen
		File repositoryRoot;
		if (args != null && args.length >= 1) {
			repositoryRoot = new File(args[0]);
		} else {
			repositoryRoot = new File(REPOSITORY_ROOT_PATH_DEFAULT);
		}

		repositoryService = new FilesystemRepositoryService(logger, repositoryRoot);
		repositoryService.init();
		settings = new Settings(logger, repositoryService, Settings.getConfigFileServer());
		messages = new Messages(logger, settings, repositoryService);
		wikiService = new WikiServiceImpl(logger, repositoryService);
		searchService = new SearchService(logger, repositoryService, wikiService, false);
		pluginService = new PluginService(logger, settings);
		htmlService = new HtmlService(logger, settings, messages, wikiService, pluginService);
		serviceLocator = new ServiceLocator(logger, settings, messages, repositoryService, wikiService, htmlService, searchService, pluginService);

		pluginService.loadPlugins(serviceLocator);

		// Server-Schicht initialisieren
		webserver = new Webserver(serviceLocator);
	}

	/**
	 * Setzt das Flag, ob der Server vom Benutzer per HTTP-Request
	 * heruntergefahren werden darf.
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
	 * Startet den Server. Der Aufruf blockiert, d.h. er kehrt erst zurück, wenn
	 * der Server beendet wird.
	 * 
	 * Der Server kann durch Aufruf von stop() von außen beendet werden.
	 */
	public void runBlocking() {
		webserver.run();
	}

	/**
	 * Startet den Server. Der Aufruf blockiert nicht, d.h. er kehrt sofort
	 * zurück.
	 */
	public void start() {
		// blockierenden Aufruf in einen extra Thread verlagern,
		// damit diese Methode sofort wieder beendet werden kann
		Thread t = new Thread("WikiServer") {
			public void run() {
				runBlocking();
			}
		};
		t.start();
	}

	/**
	 * Informiert den Server, sich zu beenden.
	 */
	public void stop() {
		webserver.stop();
	}

	/**
	 * Speicher freigeben, alles beenden.
	 * Wird von Apache Commons Daemon benötigt.
	 */
	@SuppressWarnings({"EmptyMethod"})
	public void destroy() {
	}
}
