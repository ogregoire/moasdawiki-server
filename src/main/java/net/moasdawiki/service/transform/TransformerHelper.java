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

package net.moasdawiki.service.transform;

import net.moasdawiki.service.wiki.PageElementTransformer;
import net.moasdawiki.service.wiki.structure.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper methods for transforming wiki pages.
 */
public class TransformerHelper {
    /**
     * Durchläuft den kompletten Wikibaum einer Wikiseite.
     * <p>
     * Für jedes Seitenelement wird die angegebene Callbackmethode {@link PageElementTransformer#transformPageElement(PageElement)} aufgerufen,
     * die die Möglichkeit hat, das Seitenelement durch ein neues zu ersetzen.
     *
     * @param wikiPage Wikiseite, die transformiert werden soll.
     * @param callback Objekt mit Callbackmethode, die für jeden Knoten aufgerufen wird.
     * @return Transformierte Wikiseite.
     */
    @NotNull
    public static WikiPage transformPageElements(@NotNull WikiPage wikiPage, @NotNull PageElementTransformer callback) {
        // Wiki-Baum traversieren
        PageElement pe = transformPageElement(wikiPage, callback);

        if (pe instanceof WikiPage) {
            return (WikiPage) pe;
        } else {
            return new WikiPage(wikiPage.getPagePath(), pe, wikiPage.getFromPos(), wikiPage.getToPos());
        }
    }

    /**
     * Zerlegt das angegebene Seitenelement.
     */
    @Nullable
    private static PageElement transformPageElement(@Nullable PageElement pageElement, @NotNull PageElementTransformer callback) {
        if (pageElement == null) {
            return null;
        }
        // Seitenelement durch Callbackmethode transformieren
        if (!(pageElement instanceof PageElementList)) {
            pageElement = callback.transformPageElement(pageElement);
        }

        // transformiertes Seitenelement weiter zerlegen
        if (pageElement instanceof PageElementWithChild) {
            return transformPageElementWithChild(((PageElementWithChild) pageElement), callback);
        } else if (pageElement instanceof PageElementList) {
            return transformPageElementList((PageElementList) pageElement, callback);
        } else if (pageElement instanceof Table) {
            return transformTable((Table) pageElement, callback);
        } else {
            // null / ohne Kindelemente / unbekannter Typ
            return pageElement;
        }
    }

    /**
     * Transformiert eine Liste von Seitenelementen.
     * <p>
     * Veränderte Listeneinträge werden ausgetauscht. Wenn ein Listenelement zu <code>null</code> transformiert wird, wird es aus der Liste gelöscht.
     */
    @NotNull
    private static PageElementList transformPageElementList(@NotNull PageElementList pageElementList, @NotNull PageElementTransformer callback) {
        int i = 0;
        while (i < pageElementList.size()) {
            // Listeneintrag transformieren
            PageElement pe = pageElementList.get(i);
            pe = transformPageElement(pe, callback);

            // geänderten Eintrag in Liste austauschen bzw. löschen
            if (pe != null) {
                pageElementList.set(i, pe);
                i++;
            } else {
                pageElementList.remove(i);
            }
        }
        return pageElementList;
    }

    /**
     * Transformiert das Kind-Element eines Seitenelements.
     * <p>
     * Wenn das Kind-Element ersetzt wird, wird die Parent-Referenz automatisch aktualisiert.
     */
    @NotNull
    private static PageElement transformPageElementWithChild(@NotNull PageElementWithChild pageElement, @NotNull PageElementTransformer callback) {
        PageElement child = pageElement.getChild();
        child = transformPageElement(child, callback);
        pageElement.setChild(child);
        return pageElement;
    }

    /**
     * Transformiert die Zelleninhalte einer Tabelle.
     */
    @NotNull
    private static Table transformTable(@NotNull Table table, @NotNull PageElementTransformer callback) {
        for (TableRow tableRow : table.getRows()) {
            for (TableCell tableCell : tableRow.getCells()) {
                PageElement pe = transformPageElement(tableCell.getContent(), callback);
                tableCell.setContent(pe);
            }
        }
        return table;
    }
}
