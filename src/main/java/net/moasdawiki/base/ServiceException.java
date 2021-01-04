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

package net.moasdawiki.base;

import org.jetbrains.annotations.NotNull;

/**
 * General exception class.
 */
@SuppressWarnings("serial")
public class ServiceException extends Exception {

	public ServiceException(@NotNull String message) {
		super(message);
	}

	public ServiceException(@NotNull String message, @NotNull Throwable cause) {
		super(message + ": " + cause.getMessage(), cause);
	}
}
