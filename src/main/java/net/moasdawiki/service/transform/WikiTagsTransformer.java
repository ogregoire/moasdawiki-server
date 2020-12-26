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

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.wiki.PageElementConsumer;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Collator;
import java.util.*;

/**
 * Replaces functional wiki tags by static text.
 * <p>
 * This transformer should be called as the latest tranformation step.
 */
public class WikiTagsTransformer implements TransformWikiPage {

    private static final String DATEFORMAT_DATETIME = "WikiTagsTransformer.dateformat.datetime";
    private static final String DATEFORMAT_DATE = "WikiTagsTransformer.dateformat.date";
    private static final String DATEFORMAT_TIME = "WikiTagsTransformer.dateformat.time";

    private final Logger logger;
    private final Settings settings;
    private final Messages messages;
    private final WikiService wikiService;

    /**
     * Constructor.
     */
    public WikiTagsTransformer(@NotNull Logger logger, @NotNull Settings settings,
                               @NotNull Messages messages, @NotNull WikiService wikiService) {
        this.logger = logger;
        this.settings = settings;
        this.messages = messages;
        this.wikiService = wikiService;
    }

    @NotNull
    public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
        return TransformerHelper.transformPageElements(wikiPage, this::transformPageElement);
    }

    private PageElement transformPageElement(@NotNull PageElement pageElement) {
        if (pageElement instanceof TableOfContents) {
            return transform((TableOfContents) pageElement);
        } else if (pageElement instanceof Parent) {
            return transform((Parent) pageElement);
        } else if (pageElement instanceof WikiVersion) {
            return transform((WikiVersion) pageElement);
        } else if (pageElement instanceof DateTime) {
            return transform((DateTime) pageElement);
        } else if (pageElement instanceof PageName) {
            return transform((PageName) pageElement);
        } else if (pageElement instanceof PageTimestamp) {
            return transform((PageTimestamp) pageElement);
        } else if (pageElement instanceof ListViewHistory) {
            return transform((ListViewHistory) pageElement);
        } else if (pageElement instanceof ListEditHistory) {
            return transform((ListEditHistory) pageElement);
        } else if (pageElement instanceof ListParents) {
            return transform((ListParents) pageElement);
        } else if (pageElement instanceof ListChildren) {
            return transform((ListChildren) pageElement);
        } else if (pageElement instanceof ListPages) {
            return transform((ListPages) pageElement);
        } else if (pageElement instanceof ListWantedPages) {
            return transform((ListWantedPages) pageElement);
        } else if (pageElement instanceof ListUnlinkedPages) {
            return transform((ListUnlinkedPages) pageElement);
        } else {
            return pageElement; // ansonsten unverändert lassen
        }
    }

    /**
     * Generates the table of contents (TOC) of a wiki page.
     * <p>
     * Only scans for headings of level 1 to 3.
     */
    @NotNull
    private PageElement transform(@NotNull TableOfContents tableOfContents) {
        WikiPage wikiPage = WikiHelper.getContextWikiPage(tableOfContents, false);
        if (wikiPage == null) {
            return new PageElementList();
        }

        // scan for headings
        List<Heading> headingList = new ArrayList<>();
        WikiHelper.traversePageElements(wikiPage, (heading, context) -> context.add(heading), Heading.class, headingList, false);

        // generate TOC
        PageElementList pageElementList = new PageElementList();
        int[] levelCounter = {0, 0, 0};
        for (Heading heading : headingList) {
            if (heading.getLevel() > 3) {
                continue;
            }

            // calculate heading level:
            // increase counter for current level
            levelCounter[heading.getLevel() - 1]++;
            for (int i = heading.getLevel() + 1; i <= 3; i++) {
                // reset counter for higher levels
                levelCounter[i - 1] = 0;
            }

            // concatenate numbering
            String nummerierung = "";
            for (int i = 1; i <= heading.getLevel(); i++) {
                //noinspection StringConcatenationInLoop
                nummerierung += levelCounter[i - 1] + ".";
            }

            // generate result
            PageElementList text = new PageElementList();
            text.add(new TextOnly(nummerierung + ' '));
            if (heading.getChild() != null) {
                text.add(heading.getChild());
            }
            String contentString = WikiHelper.getStringContent(heading);
            PageElement tocLine = new LinkPage(null, WikiHelper.getIdString(contentString), text, null, null);
            pageElementList.add(new Paragraph(false, heading.getLevel(), false, tocLine, null, null));
        }

        return pageElementList;
    }

    @SuppressWarnings("SameReturnValue")
    @Nullable
    private PageElement transform(@SuppressWarnings("unused") @NotNull Parent parent) {
        // ignore parent tag, it's not visible at this stage
        return null;
    }

    @NotNull
    private PageElement transform(@SuppressWarnings("unused") @NotNull WikiVersion wikiVersion) {
        return new TextOnly(settings.getProgramNameVersion());
    }

    @NotNull
    private PageElement transform(@NotNull DateTime dateTime) {
        String dateTimeFormat;
        switch (dateTime.getFormat()) {
            case SHOW_TIME:
                dateTimeFormat = messages.getMessage(DATEFORMAT_TIME);
                return new TextOnly(DateUtils.formatDate(settings.getActualTime(), dateTimeFormat));
            case SHOW_DATETIME:
                dateTimeFormat = messages.getMessage(DATEFORMAT_DATETIME);
                return new TextOnly(DateUtils.formatDate(settings.getActualTime(), dateTimeFormat));
            case SHOW_DATE:
            default:
                dateTimeFormat = messages.getMessage(DATEFORMAT_DATE);
                return new TextOnly(DateUtils.formatDate(settings.getActualTime(), dateTimeFormat));
        }
    }

    @Nullable
    private PageElement transform(@NotNull PageName pageName) {
        WikiPage wikiPage = WikiHelper.getContextWikiPage(pageName, pageName.isGlobalContext());
        if (wikiPage == null) {
            return null;
        }
        String pagePath = wikiPage.getPagePath();
        if (pagePath == null) {
            return null;
        }

        if (pageName.getPageNameFormat() == Listable.PageNameFormat.PAGE_TITLE) {
            String pageTitle = PathUtils.extractWebName(pagePath);
            if (pageName.isLinked()) {
                return new LinkPage(pagePath, new TextOnly(pageTitle));
            } else {
                return new TextOnly(pageTitle);
            }
        } else if (pageName.getPageNameFormat() == Listable.PageNameFormat.PAGE_FOLDER) {
            String pageFolder = PathUtils.extractWebFolder(pagePath);
            if (pageName.isLinked()) {
                return generateStepwiseLinks(pageFolder);
            } else {
                return new TextOnly(pageFolder);
            }
        } else {
            if (pageName.isLinked()) {
                return generateStepwiseLinks(pagePath);
            } else {
                return new TextOnly(pagePath);
            }
        }
    }

    /**
     * Generated linked sections of a page path.
     */
    @NotNull
    private PageElement generateStepwiseLinks(@NotNull String path) {
        PageElementList result = new PageElementList();

        path = PathUtils.makeWebPathAbsolute(path, null);
        int leftPos = 0;
        while (leftPos < path.length()) {
            // next section
            int rightPos = path.indexOf('/', leftPos);
            if (rightPos < 0) {
                // last section
                rightPos = path.length() - 1;
            }

            // determine the name of the path of a section
            // including trailing '/' if available
            String partName = path.substring(leftPos, rightPos + 1);
            String partPath = path.substring(0, rightPos + 1);

            // generate link
            if (leftPos > 0) {
                result.add(new TextOnly(" "));
            }
            result.add(new LinkPage(partPath, new TextOnly(partName)));

            leftPos = rightPos + 1;
        }

        return result;
    }

    @Nullable
    private PageElement transform(@NotNull PageTimestamp pageTimestamp) {
        WikiPage wikiPage = WikiHelper.getContextWikiPage(pageTimestamp, pageTimestamp.isGlobalContext());
        if (wikiPage == null || wikiPage.getPagePath() == null) {
            return null;
        }

        try {
            WikiFile wikiFile = wikiService.getWikiFile(wikiPage.getPagePath());
            String dateTimeFormat = messages.getMessage(DATEFORMAT_DATETIME);
            return new TextOnly(DateUtils.formatDate(wikiFile.getRepositoryFile().getContentTimestamp(), dateTimeFormat));
        } catch (ServiceException e) {
            logger.write("Error reading wiki file to show page timestamp", e);
            return null;
        }
    }

    @Nullable
    private PageElement transform(@NotNull ListViewHistory listViewHistory) {
        List<String> history = wikiService.getLastViewedWikiFiles(listViewHistory.getMaxLength());
        return generateListOfPageLinks(history, listViewHistory);
    }

    @Nullable
    private PageElement transform(@NotNull ListEditHistory listEditHistory) {
        List<String> history = wikiService.getLastModified(listEditHistory.getMaxLength());
        return generateListOfPageLinks(history, listEditHistory);
    }

    @Nullable
    private PageElement transform(@NotNull ListParents listParents) {
        WikiPage wikiPage = WikiHelper.getContextWikiPage(listParents, listParents.isGlobalContext());

        // determine absolute path
        String pagePath = listParents.getPagePath();
        pagePath = WikiHelper.getAbsolutePagePath(pagePath, wikiPage);
        if (pagePath == null) {
            return null;
        }

        // get parent pages and sort by name
        try {
            WikiFile wikiFile = wikiService.getWikiFile(pagePath);
            List<String> list = new ArrayList<>(wikiFile.getParents());
            list.sort(Collator.getInstance(Locale.GERMAN));
            return generateListOfPageLinks(list, listParents);
        } catch (ServiceException e) {
            // in case of an artificial page show nothing
            logger.write("Error reading wiki page to list parents", e);
            return null;
        }
    }

    @Nullable
    private PageElement transform(@NotNull ListChildren listChildren) {
        WikiPage wikiPage = WikiHelper.getContextWikiPage(listChildren, listChildren.isGlobalContext());

        // determine absolute path
        String pagePath = listChildren.getPagePath();
        pagePath = WikiHelper.getAbsolutePagePath(pagePath, wikiPage);
        if (pagePath == null) {
            return null;
        }

        // get child pages and sort by name
        try {
            WikiFile wikiFile = wikiService.getWikiFile(pagePath);
            List<String> list = new ArrayList<>(wikiFile.getChildren());
            list.sort(Collator.getInstance(Locale.GERMAN));
            return generateListOfPageLinks(list, listChildren);
        } catch (ServiceException e) {
            // in case of an artificial page show nothing
            logger.write("Error reading wiki page to list children", e);
            return null;
        }
    }

    @Nullable
    private PageElement transform(@NotNull ListPages listPages) {
        WikiPage wikiPage = WikiHelper.getContextWikiPage(listPages, listPages.isGlobalContext());
        if (wikiPage == null) {
            return null;
        }

        // determine absolute path
        String folder = listPages.getFolder();
        if (folder == null) {
            folder = PathUtils.extractWebFolder(wikiPage.getPagePath());
        }
        folder = WikiHelper.getAbsolutePagePath(folder, wikiPage);

        // get list of all wiki pages
        Set<String> allPages = wikiService.getWikiFilePaths();

        // filter list
        List<String> list = new ArrayList<>(allPages.size());
        for (String pagePath : allPages) {
            if (folder == null || PathUtils.extractWebFolder(pagePath).startsWith(folder)) {
                list.add(pagePath);
            }
        }
        list.sort(Collator.getInstance(Locale.GERMAN));
        return generateListOfPageLinks(list, listPages);
    }

    @Nullable
    private PageElement transform(@NotNull ListWantedPages listWantedPages) {
        Set<String> allPagePaths = wikiService.getWikiFilePaths();

        // remove working links
        Set<String> allPageLinks = extractAllPageLinks(allPagePaths);
        allPageLinks.removeAll(allPagePaths);

        // remove links to index pages
        allPageLinks.removeIf(pagePath -> pagePath.endsWith("/"));

        // sort list
        List<String> wantedPagePathsList = new ArrayList<>(allPageLinks);
        wantedPagePathsList.sort(Collator.getInstance(Locale.GERMAN));

        return generateListOfPageLinks(wantedPagePathsList, listWantedPages);
    }

    @Nullable
    private PageElement transform(@NotNull ListUnlinkedPages listUnlinkedPages) {
        Set<String> allPagePaths = wikiService.getWikiFilePaths();

        // remove linked pages
        Set<String> result = new HashSet<>(allPagePaths);
        Set<String> allPageLinks = extractAllPageLinks(allPagePaths);
        String indexPageName = settings.getIndexPageName();
        for (String pagePath : allPageLinks) {
            // index pages can be linked implicitly -> resolve them
            if (pagePath.endsWith("/") && indexPageName != null) {
                result.remove(pagePath + indexPageName);
            } else {
                result.remove(pagePath);
            }
        }

        // remove parent and child pages as they are considered to be linked
        for (String pagePath : allPagePaths) {
            try {
                WikiFile wikiFile = wikiService.getWikiFile(pagePath);
                if (listUnlinkedPages.isHideParents()) {
                    result.removeAll(wikiFile.getParents());
                }
                if (listUnlinkedPages.isHideChildren()) {
                    result.removeAll(wikiFile.getChildren());
                }
            } catch (ServiceException e) {
                logger.write("Error reading wiki file to get parents and children, ignoring it", e);
            }
        }

        // remove start page as it is considered to be linked
        result.remove(settings.getStartpagePath());

        // sort list
        List<String> unlinkedPagePathsList = new ArrayList<>(result);
        unlinkedPagePathsList.sort(Collator.getInstance(Locale.GERMAN));

        return generateListOfPageLinks(unlinkedPagePathsList, listUnlinkedPages);
    }

    /**
     * Scans the listed wiki pages and collects the links to wiki pages.
     */
    @NotNull
    private Set<String> extractAllPageLinks(@NotNull Set<String> pagePaths) {
        Set<String> linkedPagePaths = new HashSet<>();
        PageElementConsumer<LinkPage, Set<String>> consumer = (linkPage, context) -> {
            WikiPage contextWikiPage = WikiHelper.getContextWikiPage(linkPage, false);
            if (contextWikiPage != null) {
                String absolutePagePath = WikiHelper.getAbsolutePagePath(linkPage.getPagePath(), contextWikiPage);
                context.add(absolutePagePath);
            }
        };
        for (String pagePath : pagePaths) {
            try {
                WikiFile wikiFile = wikiService.getWikiFile(pagePath);
                WikiHelper.traversePageElements(wikiFile.getWikiPage(), consumer, LinkPage.class, linkedPagePaths, true);
            } catch (ServiceException e) {
                logger.write("Error reading wiki page to scan for links, ignoring it", e);
            }
        }
        return linkedPagePaths;
    }

    /**
     * Returns a list of wiki pages. The items are linked to the corresponding
     * wiki page.
     */
    @Nullable
    private PageElement generateListOfPageLinks(@NotNull List<String> pagePaths, @NotNull Listable listoptions) {
        // list empty? -> return text hint
        if (pagePaths.isEmpty()) {
            if (listoptions.getOutputOnEmpty() != null) {
                return new TextOnly(listoptions.getOutputOnEmpty());
            } else {
                return null;
            }
        } else {
            PageElementList pageElementList = new PageElementList();
            for (int i = 0; i < pagePaths.size(); i++) {
                String pagePath = pagePaths.get(i);

                // concatenate link
                String pageName;
                switch (listoptions.getPageNameFormat()) {
                    case PAGE_FOLDER:
                        pageName = PathUtils.extractWebFolder(pagePath);
                        break;
                    case PAGE_TITLE:
                        pageName = PathUtils.extractWebName(pagePath);
                        break;
                    default:
                        pageName = pagePath;
                }
                LinkPage linkPage = new LinkPage(pagePath, new TextOnly(pageName));

                // generate list entries
                if (listoptions.isShowInline()) {
                    // list entries are inline
                    if (i > 0 && listoptions.getInlineListSeparator() != null) {
                        pageElementList.add(new TextOnly(listoptions.getInlineListSeparator()));
                    }
                    pageElementList.add(linkPage);
                } else {
                    // list entries as bullet points
                    pageElementList.add(new ListItem(1, false, linkPage, null, null));
                }
            }
            return pageElementList;
        }
    }
}
