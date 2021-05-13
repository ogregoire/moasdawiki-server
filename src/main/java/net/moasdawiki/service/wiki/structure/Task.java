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

package net.moasdawiki.service.wiki.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Beschreibt eine Aufgabe (Task) in einer Wiki-Seite.
 */
public class Task extends PageElement {

	/**
	 * Status der Aufgabe.
	 */
	@Nullable
	private State state;

	/**
	 * Terminhinweis zur Aufgabe. null -> kein Termin.
	 */
	@Nullable
	private final String schedule;

	/**
	 * Beschreibungstext der Aufgabe. null -> kein Beschreibungstext vorhanden.
	 */
	@Nullable
	private final String description;

	public Task(@Nullable State state, @Nullable String schedule, @Nullable String description, @Nullable Integer fromPos, @Nullable Integer toPos) {
		super();
		this.fromPos = fromPos;
		this.toPos = toPos;
		this.state = state;
		this.schedule = schedule;
		this.description = description;
	}

	/**
	 * Gibt den Status der Aufgabe zurück.
	 */
	@Nullable
	public State getState() {
		return state;
	}

	/**
	 * Setzt den Status der Aufgabe.
	 */
	@SuppressWarnings("unused")
	public void setState(@Nullable State state) {
		this.state = state;
	}

	/**
	 * Gibt den Terminhinweis zur Aufgabe zurück.
	 * 
	 * @return null -> kein Terminhinweis vorhanden.
	 */
	@Nullable
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Gibt den Beschreibungstext der Aufgabe zurück.
	 * 
	 * @return null -> kein Beschreibungstext vorhanden.
	 */
	@Nullable
	public String getDescription() {
		return description;
	}

	public boolean isInline() {
		return false;
	}

	@NotNull
	public PageElement clonePageElement() {
		return new Task(state, schedule, description, fromPos, toPos);
	}

	/**
	 * Status einer Aufgabe.
	 */
	public enum State {
		OPEN, OPEN_IMPORTANT, CLOSED
	}
}
