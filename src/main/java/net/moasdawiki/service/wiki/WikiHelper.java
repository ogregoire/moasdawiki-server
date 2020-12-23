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

package net.moasdawiki.service.wiki;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stellt Hilfsmethoden zur Traversierung und Transformation von Wikiseiten zur Verfügung.
 */
public abstract class WikiHelper {

    /**
     * Ergänzt eine Wikiseite um die Navigation, Header und Footer.
     *
     * @param wikiPage      Die zu ergänzende Wikiseite.
     * @param addNavigation Soll die Navigation eingefügt werden? Diese kommt aus einer Wikiseite, die über "page.navigation" in config.txt festgelegt wird.
     * @param addHeader     Soll der Header eingefügt werden? Dieser kommt aus einer Wikiseite, die über "page.header" in config.txt festgelegt wird.
     * @param addFooter     Soll der Footer eingefügt werden? Dieser kommt aus einer Wikiseite, die über "page.footer" in config.txt festgelegt wird.
     * @return Die erweiterte Wikiseite.
     */
    @NotNull
    public static WikiPage extendWikiPage(@NotNull WikiPage wikiPage, boolean addNavigation, boolean addHeader,
                                          boolean addFooter, @NotNull Logger logger, @NotNull Settings settings,
                                          @NotNull WikiService wikiService) {
        PageElementList content = new PageElementList();
        if (addNavigation && settings.getNavigationPagePath() != null) {
            try {
                WikiFile navigationWikiFile = wikiService.getWikiFile(settings.getNavigationPagePath());
                content.add(new Html("<div class=\"menu\">"));
                content.add(navigationWikiFile.getWikiPage());
                content.add(new Html("</div>"));
            } catch (ServiceException e) {
                logger.write("Error reading navigation page to assemble an integrated page, ignoring it", e);
            }
        }

        content.add(new Html("<div class=\"wikipage\">"));
        if (addHeader && settings.getHeaderPagePath() != null) {
            try {
                WikiFile headerWikiFile = wikiService.getWikiFile(settings.getHeaderPagePath());
                content.add(headerWikiFile.getWikiPage());
            } catch (ServiceException e) {
                logger.write("Error reading header page to assemble an integrated page, ignoring it", e);
            }
        }
        content.add(wikiPage);

        if (addFooter && settings.getFooterPagePath() != null) {
            try {
                WikiFile footerWikiFile = wikiService.getWikiFile(settings.getFooterPagePath());
                content.add(footerWikiFile.getWikiPage());
            } catch (ServiceException e) {
                logger.write("Error reading footer page to assemble an integrated page, ignoring it", e);
            }
        }
        content.add(new Html("</div>"));

        return new WikiPage(wikiPage.getPagePath(), content, null, null);
    }

    /**
     * Traverses a wiki tree and calls the consumer method for every matching node.
     *
     * @param pageElement              Tree root node.
     * @param consumer                 Consumer method to call for every matching node.
     * @param nodeType                 Tree node type of interest. Required as Java doesn't have Generics type information at runtime.
     * @param recurseIntoMatchingNodes <code>true</code> -> traverse into nodes of type <code>nodeType</code>.
     */
    public static <T extends PageElement, C> void traversePageElements(@NotNull PageElement pageElement,
                                                                       @NotNull PageElementConsumer<T, C> consumer,
                                                                       @NotNull Class<T> nodeType,
                                                                       @NotNull C context,
                                                                       boolean recurseIntoMatchingNodes) {
        if (nodeType.isInstance(pageElement)) {
            // Call consumer method
			consumer.consume(nodeType.cast(pageElement), context);

            if (!recurseIntoMatchingNodes) {
                // cancel recursion
                return;
            }
        }

        // traverse recursively all child nodes
        if (pageElement instanceof PageElementWithChild) {
            PageElement child = ((PageElementWithChild) pageElement).getChild();
            if (child != null) {
                traversePageElements(child, consumer, nodeType, context, recurseIntoMatchingNodes);
            }
        } else if (pageElement instanceof PageElementList) {
            for (PageElement pe : (PageElementList) pageElement) {
                traversePageElements(pe, consumer, nodeType, context, recurseIntoMatchingNodes);
            }
        } else if (pageElement instanceof Table) {
            for (TableRow row : ((Table) pageElement).getRows()) {
                for (TableCell cell : row.getCells()) {
                    if (cell.getContent() != null) {
                        traversePageElements(cell.getContent(), consumer, nodeType, context, recurseIntoMatchingNodes);
                    }
                }
            }
        }
    }

    /**
     * Sucht den lokalen bzw. globalen Kontext zu einem Knoten im Wikibaum.
     * <p>
     * Der lokale Kontext ist die Wikiseite, die das Seitenelement enthält.
     * Der globale Kontext ist die Wurzel des Baums.
     * Der globale und lokale Kontext zu einem Knoten unterscheiden sich nur,
     * wenn eine Wikiseite per <tt>includepage</tt> in eine andere eingebettet wird.
     *
     * @param pageElement   Der Knoten im Wikibaum, zu dem der globale/lokale Kontext gesucht werden soll.
     * @param globalContext <code>true</code> -> globalen Kontext suchen, <code>false</code> -> lokalen Kontext suchen.
     * @return Der Kontext zum Knoten. <code>null</code> -> keinen Kontext gefunden. Sollte eigentlich nicht passieren, da jeder Wikibaum ein WikiPage-Element als Wurzel haben sollte.
     */
    @Nullable
    public static WikiPage getContextWikiPage(@Nullable PageElement pageElement, boolean globalContext) {
        while (pageElement != null) {
            // Kontext gefunden?
            if (pageElement instanceof WikiPage && (!globalContext || pageElement.getParent() == null)) {
                return (WikiPage) pageElement;
            }

            pageElement = pageElement.getParent();
        }

        // Keinen gültigen Kontext gefunden.
        return null;
    }

    /**
     * Wandelt den angegebenen Pfad einer Wikiseite in einen absoluten Pfad um.
     *
     * @param pagePath        Pfad im Repository. Relativ oder absolut.
     *                        <code>null</code> -> aktuelle Wikiseite verwenden.
     * @param contextWikiPage Die Kontext-Wikiseite.
     * @return Absoluter Pfad.
     */
    @Nullable
    public static String getAbsolutePagePath(@Nullable String pagePath, @Nullable WikiPage contextWikiPage) {
        if (contextWikiPage == null) {
            return null;
        }
        if (pagePath != null) {
            return PathUtils.makeWebPathAbsolute(pagePath, PathUtils.extractWebFolder(contextWikiPage.getPagePath()));
        }
        return contextWikiPage.getPagePath();
    }

    /**
     * Gibt den reinen Textanteil im angegebenen Teilbaum zurück.
     * Das sind alle TextOnly-Inhalte aneinander gekettet.
     *
     * @param pageElement Teilbaum, aus dem der Text extrahiert werden soll.
     * @return Enthaltener reiner Text.
     */
    @NotNull
    public static String getStringContent(@NotNull PageElement pageElement) {
		StringBuilder sb = new StringBuilder();
		WikiHelper.traversePageElements(pageElement, (textOnly, context) -> context.append(textOnly.getText()), TextOnly.class, sb, true);
        return sb.toString();
    }

    /**
     * Entfernt aus einem beliebigen String alle Sonderzeichen, damit ein String
     * entsteht, der als HTML-id verwendet werden kann.
     *
     * @param s Beliebiger String.
     * @return id String.
     */
    @NotNull
    public static String getIdString(@NotNull String s) {
        StringBuilder sb = new StringBuilder();
        boolean letterPrefix = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_') {
                letterPrefix = true;
            }
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || letterPrefix
                    && ((ch >= '0' && ch <= '9') || ch == '-' || ch == '.' || ch == ':')) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
