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

package net.moasdawiki.service.wiki;

import java.util.HashSet;
import java.util.Set;

import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;

/**
 * Enthält den Inhalt einer Wikidatei.
 * 
 * @author Herbert Reiter
 */
public class WikiFile {

	/**
	 * Name der Wikidatei, ohne Dateiendung. Nicht <code>null</code>.
	 */
	@NotNull
	private final String wikiFilePath;

	/**
	 * Der Inhalt der Wikidatei, Text in Wikisyntax. Nicht <code>null</code> außerhalb WikiService.
	 */
	@NotNull
	private final String wikiText;

	/**
	 * Geparster Inhalt der Wikidatei. Nicht <code>null</code> außerhalb WikiService.
	 */
	@NotNull
	private final WikiPage wikiPage;

	/**
	 * Vaterseiten dieser Wikidatei, ohne Dateiendung. Nicht <code>null</code>.
	 */
	@NotNull
	private final Set<String> parents;

	/**
	 * Kindseiten dieser Wikidatei, ohne Dateiendung. Nicht <code>null</code>.
	 */
	@NotNull
	private final Set<String> children;

	/**
	 * Dateiname und Zeitstempel im Repository. Nicht <code>null</code>.
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
	 * Gibt den Namen der Wikidatei zurück, ohne Dateiendung. Nicht
	 * <code>null</code>.
	 */
	@NotNull
	public String getWikiFilePath() {
		return wikiFilePath;
	}

	/**
	 * Gibt den Inhalt der Wikidatei zurück. Der Text ist in Wikisyntax
	 * verfasst. Nicht <code>null</code>.
	 */
	@NotNull
	public String getWikiText() {
		return wikiText;
	}

	/**
	 * Gibt den geparsten Inhalt der Wikidatei zurück. Nicht <code>null</code>.
	 */
	@NotNull
	public WikiPage getWikiPage() {
		return wikiPage;
	}

	/**
	 * Gibt die Vaterseiten dieser Wikidatei zurück, ohne Dateiendung. Nicht
	 * <code>null</code>. Darf nicht modifiziert werden.
	 */
	@NotNull
	public Set<String> getParents() {
		return parents;
	}

	/**
	 * Gibt die Kindseiten dieser Wikidatei zurück, ohne Dateiendung. Nicht
	 * <code>null</code>.
	 */
	@NotNull
	public Set<String> getChildren() {
		return children;
	}

	/**
	 * Gibt den Dateinamen und Zeitstempel im Repository zurück. Nicht
	 * <code>null</code>.
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
