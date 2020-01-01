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

package net.moasdawiki.plugin;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.render.HtmlService;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.search.SearchService;
import net.moasdawiki.service.wiki.WikiService;
import org.jetbrains.annotations.NotNull;

/**
 * Stellt Zugriff auf alle Service-Klassen bereit. Diese bilden die API von
 * MoasdaWiki, die u.a. Plugins zugänglich gemacht wird.
 * 
 * @author Herbert Reiter
 */
public class ServiceLocator {

	@NotNull
	private final Logger logger;
	@NotNull
	private final Settings settings;
	@NotNull
	private final Messages messages;
	@NotNull
	private final RepositoryService repositoryService;
	@NotNull
	private final WikiService wikiService;
	@NotNull
	private final HtmlService htmlService;
	@NotNull
	private final SearchService searchService;
	@NotNull
	private final PluginService pluginService;

	/**
	 * Konstruktor.
	 */
	public ServiceLocator(@NotNull Logger logger, @NotNull Settings settings, @NotNull Messages messages, @NotNull RepositoryService repositoryService,
						  @NotNull WikiService wikiService, @NotNull HtmlService htmlService, @NotNull SearchService searchService, @NotNull PluginService pluginService) {
		this.logger = logger;
		this.settings = settings;
		this.messages = messages;
		this.repositoryService = repositoryService;
		this.wikiService = wikiService;
		this.htmlService = htmlService;
		this.searchService = searchService;
		this.pluginService = pluginService;
	}

	/**
	 * Gibt den Logger zurück.
	 */
	@NotNull
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Gibt die Einstellungen zurück.
	 */
	@NotNull
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Gibt die Übersetzungen zurück.
	 */
	@NotNull
	public Messages getMessages() {
		return messages;
	}

	/**
	 * Gibt den FilesystemRepositoryService zurück.
	 */
	@NotNull
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	/**
	 * Gibt den WikiService zurück.
	 */
	@NotNull
	public WikiService getWikiService() {
		return wikiService;
	}

	/**
	 * Gibt den HtmlService zurück.
	 */
	@NotNull
	public HtmlService getHtmlService() {
		return htmlService;
	}

	/**
	 * Gibt den SearchService zurück.
	 */
	@NotNull
	public SearchService getSearchService() {
		return searchService;
	}

	/**
	 * Gibt den PluginService zurück.
	 */
	@NotNull
	public PluginService getPluginService() {
		return pluginService;
	}
}
