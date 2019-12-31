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

package net.moasdawiki.plugin;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.wiki.PageElementTransformer;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.IncludePage;
import net.moasdawiki.service.wiki.structure.PageElement;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Löst alle include-Tags auf und ersetzt sie durch die entsprechende
 * Unterseite.<br>
 * <br>
 * Dieses Plugin sollte als erstes Transformations-Plugin aufgerufen werden,
 * damit alle nachfolgenden Plugins die komplette Wikiseite samt Unterseiten
 * korrekt transformieren können.
 * 
 * @author Herbert Reiter
 */
public class IncludePagePlugin implements Plugin, PageElementTransformer {

	private Logger logger;
	private WikiService wikiService;

	public void setServiceLocator(@NotNull ServiceLocator serviceLocator) {
		this.logger = serviceLocator.getLogger();
		this.wikiService = serviceLocator.getWikiService();
	}

	@NotNull
	@CallOrder(1)
	public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
		return WikiHelper.transformPageElements(wikiPage, this);
	}

	@Nullable
	public PageElement transformPageElement(@NotNull PageElement pageElement) {
		if (pageElement instanceof IncludePage) {
			// Unterseite laden
			WikiPage wikiPage = WikiHelper.getContextWikiPage(pageElement, false);
			IncludePage includePage = (IncludePage) pageElement;
			String pagePath = WikiHelper.getAbsolutePagePath(includePage.getPagePath(), wikiPage);
			try {
				WikiFile subWikiFile = wikiService.getWikiFile(pagePath);
				return subWikiFile.getWikiPage();
			}
			catch (ServiceException e) {
				logger.write("Cannot embed wiki page '" + includePage.getPagePath() + "' as it doesn't exist");
				return null; // Include-Tag löschen
			}
		} else {
			return pageElement;
		}
	}
}
