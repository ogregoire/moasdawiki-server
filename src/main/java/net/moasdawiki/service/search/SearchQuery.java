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

package net.moasdawiki.service.search;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Search request data.
 * 
 * @author Herbert Reiter
 */
public class SearchQuery {
	/**
	 * Original search query string.
	 */
	@NotNull
	private final String queryString;
	
	/**
	 * Words that must match.
	 */
	@NotNull
	private final Set<String> included;

	/**
	 * Words that must not match.
	 * They have a higher priority than those in {@link #included}.
	 */
	@NotNull
	private final Set<String> excluded;

	/**
	 * Constructor.
	 *
	 * @param queryString Original search query string.
	 * @param included Words that must match.
	 * @param excluded Words that must not match.
	 */
	@Contract(pure = true)
	public SearchQuery(@NotNull String queryString, @NotNull Set<String> included, @NotNull Set<String> excluded) {
		this.queryString = queryString;
		this.included = included;
		this.excluded = excluded;
	}

	@Contract(pure = true)
	@NotNull
	public String getQueryString() {
		return queryString;
	}

	@Contract(pure = true)
	@NotNull
	public Set<String> getIncluded() {
		return included;
	}

	@Contract(pure = true)
	@NotNull
	public Set<String> getExcluded() {
		return excluded;
	}
}
