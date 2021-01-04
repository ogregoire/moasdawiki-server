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

package net.moasdawiki.service.repository;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Represents a (binary) file in the wiki repository.
 */
public class AnyFile {

    /**
     * Absolute file path inside the repository including the file name ending.
     */
    @NotNull
    private final String filePath;

    /**
     * Last change timestamp of the file.
     * Is identical to the file system timestamp most times
     * but can differ after synchronization of a client with the server.
     */
    @NotNull
    private final Date contentTimestamp;

    public AnyFile(@NotNull String filePath) {
        this(filePath, new Date());
    }

    public AnyFile(@NotNull String filePath, @NotNull Date contentTimestamp) {
        super();
        this.filePath = filePath;
        this.contentTimestamp = contentTimestamp;
    }

    @NotNull
    public String getFilePath() {
        return filePath;
    }

    @NotNull
    public Date getContentTimestamp() {
        return contentTimestamp;
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (o instanceof AnyFile) {
            AnyFile other = (AnyFile) o;
            return filePath.equals(other.filePath);
        } else {
            return false;
        }
    }
}
