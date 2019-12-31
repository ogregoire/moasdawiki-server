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

package net.moasdawiki.service.search;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO mit der Suchabfrage. Diese kann mehrere ein- und auszuschließende
 * Textphrasen enthalten.
 * 
 * @author Herbert Reiter
 */
public class SearchQuery {
	/**
	 * Ursprünglicher Query-String. Nicht <code>null</code>.
	 */
	public String queryString;
	
	/**
	 * Textphrasen, die auf einer Wikiseite vorkommen müssen. Nicht
	 * <code>null</code>.
	 */
	@NotNull
	public final Set<String> included = new HashSet<>();

	/**
	 * Textphrasen, die nicht vorkommen dürfen. Diese haben eine höhere
	 * Priorität als die in {@link #included}. Nicht <code>null</code>.
	 */
	@NotNull
	public final Set<String> excluded = new HashSet<>();
}
