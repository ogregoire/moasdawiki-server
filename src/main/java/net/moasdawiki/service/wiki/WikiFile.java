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

package net.moasdawiki.service.wiki;

import java.util.HashSet;
import java.util.Set;

import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;

/**
 * Enthält den Inhalt einer Wikidatei.
 */
public class WikiFile {

	/**
	 * Name der Wikidatei, ohne Dateiendung.
	 */
	@NotNull
	private final String wikiFilePath;

	/**
	 * Der Inhalt der Wikidatei, Text in Wikisyntax.
	 */
	@NotNull
	private final String wikiText;

	/**
	 * Geparster Inhalt der Wikidatei.
	 */
	@NotNull
	private final WikiPage wikiPage;

	/**
	 * Vaterseiten dieser Wikidatei, ohne Dateiendung.
	 */
	@NotNull
	private final Set<String> parents;

	/**
	 * Kindseiten dieser Wikidatei, ohne Dateiendung.
	 */
	@NotNull
	private final Set<String> children;

	/**
	 * Dateiname und Zeitstempel im Repository.
	 */
	@NotNull
	private final AnyFile repositoryFile;

	/**
	 * Konstruktor.
	 */
	public WikiFile(@NotNull String wikiFilePath, @NotNull String wikiText, @NotNull WikiPage wikiPage, @NotNull AnyFile repositoryFile) {
		super();
		this.wikiFilePath = wikiFilePath;
		this.wikiText = wikiText;
		this.wikiPage = wikiPage;
		this.repositoryFile = repositoryFile;
		this.parents = new HashSet<>();
		this.children = new HashSet<>();
	}

	/**
	 * Gibt den Namen der Wikidatei zurück, ohne Dateiendung.
	 */
	@NotNull
	public String getWikiFilePath() {
		return wikiFilePath;
	}

	/**
	 * Gibt den Inhalt der Wikidatei zurück. Der Text ist in Wikisyntax verfasst.
	 */
	@NotNull
	public String getWikiText() {
		return wikiText;
	}

	/**
	 * Gibt den geparsten Inhalt der Wikidatei zurück.
	 */
	@NotNull
	public WikiPage getWikiPage() {
		return wikiPage;
	}

	/**
	 * Gibt die Vaterseiten dieser Wikidatei zurück, ohne Dateiendung. Darf nicht modifiziert werden.
	 */
	@NotNull
	public Set<String> getParents() {
		return parents;
	}

	/**
	 * Gibt die Kindseiten dieser Wikidatei zurück, ohne Dateiendung.
	 */
	@NotNull
	public Set<String> getChildren() {
		return children;
	}

	/**
	 * Gibt den Dateinamen und Zeitstempel im Repository zurück.
	 */
	@NotNull
	public AnyFile getRepositoryFile() {
		return repositoryFile;
	}

	/**
	 * Erstellt eine tiefe Kopie dieses Objekts.
	 */
	@NotNull
	public WikiFile cloneTyped() {
		WikiFile result = new WikiFile(wikiFilePath, wikiText, wikiPage.cloneTyped(), repositoryFile);
		result.parents.addAll(parents);
		result.children.addAll(children);
		return result;
	}
}
