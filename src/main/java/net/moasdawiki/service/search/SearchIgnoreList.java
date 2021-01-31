/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
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

package net.moasdawiki.service.search;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads the search word ignore list.
 */
public class SearchIgnoreList {

    static final String SEARCH_IGNORE_LIST_FILEPATH = "/search-ignore-list.text";

    @NotNull
    private final Logger logger;

    @NotNull
    private final RepositoryService repositoryService;

    /**
     * Contains the ignore list words.
     * null -> list not loaded yet.
     */
    @NotNull
    private final Set<String> ignoreList;

    /**
     * Constructor.
     */
    public SearchIgnoreList(@NotNull Logger logger, @NotNull RepositoryService repositoryService) {
        this.logger = logger;
        this.repositoryService = repositoryService;
        this.ignoreList = new HashSet<>();
    }

    /**
     * Drop the cache content and reread the ignore list on next access.
     * Is called in App environment after synchronization with server.
     */
    public void reset() {
        ignoreList.clear();
    }

    /**
     * Check if the given word is not on the ignore list.
     * The word is not normalized.
     */
    @Contract(pure = true)
    public boolean isValidWord(@NotNull String word) {
        if (word.length() <= 1) {
            return false;
        }

        // lazy load list
        if (ignoreList.isEmpty()) {
            readList();
        }
        return !ignoreList.contains(word);
    }

    /**
     * Reads the ignore list.
     * Every line contains one word.
     */
    private void readList() {
        try {
            AnyFile ignoreListFile = new AnyFile(SEARCH_IGNORE_LIST_FILEPATH);
            String ignoreListContent = repositoryService.readTextFile(ignoreListFile);

            try (BufferedReader reader = new BufferedReader(new StringReader(ignoreListContent))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty()) {
                        ignoreList.add(line);
                    }
                }
                logger.write(ignoreList.size() + " words read from ignore list file");
            }
        } catch (ServiceException | IOException e) {
            logger.write("Error reading ignore list file");
        }
    }
}
