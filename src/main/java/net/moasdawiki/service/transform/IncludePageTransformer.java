/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.service.transform;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.IncludePage;
import net.moasdawiki.service.wiki.structure.PageElement;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Replaces all <code>{{includepage:pagePath}}</code> tags
 * ({@link IncludePage}) by the content of the referenced wiki page.
 * <p>
 * This transformer should be called before other transformers. This ensures
 * that other transformers will replace all tags including those in sub pages.
 */
public class IncludePageTransformer implements TransformWikiPage {

    private final Logger logger;
    private final WikiService wikiService;

    /**
     * Constructor.
     */
    public IncludePageTransformer(@NotNull Logger logger, @NotNull WikiService wikiService) {
        this.logger = logger;
        this.wikiService = wikiService;
    }

    @NotNull
    public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
        return TransformerHelper.transformPageElements(wikiPage, this::transformPageElement);
    }

    @Nullable
    private PageElement transformPageElement(@NotNull PageElement pageElement) {
        if (pageElement instanceof IncludePage) {
            // Read sub page
            WikiPage wikiPage = WikiHelper.getContextWikiPage(pageElement, false);
            IncludePage includePage = (IncludePage) pageElement;
            String pagePath = WikiHelper.getAbsolutePagePath(includePage.getPagePath(), wikiPage);
            if (pagePath == null) {
                return null;
            }
            try {
                WikiFile subWikiFile = wikiService.getWikiFile(pagePath);
                return subWikiFile.getWikiPage();
            } catch (ServiceException e) {
                logger.write("Cannot embed wiki page '" + includePage.getPagePath() + "' as it doesn't exist");
                // remove "includepage" tag in case of an error
                return null;
            }
        } else {
            return pageElement;
        }
    }
}
