/*
 * MoasdaWiki Server
 *
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation (AGPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <https://www.gnu.org/licenses/agpl-3.0.html>.
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
