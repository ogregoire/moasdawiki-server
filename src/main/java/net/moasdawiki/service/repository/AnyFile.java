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

package net.moasdawiki.service.repository;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * Repräsentiert eine (Binär-)Datei im Wiki-Repository.
 *
 * @author Herbert Reiter
 */
public class AnyFile {

    /**
     * Der absolute Pfad der Datei innerhalb des Repositories inkl. Dateiendung.
     * Nicht <code>null</code>.
     */
    @NotNull
    private final String filePath;

    /**
     * Gibt den Zeitstempel der letzten Inhaltsänderung der Datei zurück. Ist
     * i.d.R. identisch mit Datei-Zeitstempel des Dateisystems. Beim Synchronisieren
     * von Dateien wird der Zeitstempel allerdings auf denselben Wert wie der Synchronisierungspartner gesetzt.
     * <code>null</code> --> Zeitstempel unbekannt.
     */
    @Nullable
    private final Date contentTimestamp;

    /**
     * Konstruktor.
     */
    public AnyFile(@NotNull String filePath, @Nullable Date contentTimestamp) {
        super();
        this.filePath = filePath;
        this.contentTimestamp = contentTimestamp;
    }

    /**
     * Gibt den absoluten Pfad der Datei innerhalb des Repositories inkl.
     * Dateiendung zurück. Nicht <code>null</code>.
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }

	/**
	 * Gibt den Zeitstempel der letzten Inhaltsänderung der Datei zurück. Ist
	 * i.d.R. identisch mit Datei-Zeitstempel des Dateisystems. Beim Synchronisieren
	 * von Dateien wird der Zeitstempel allerdings auf denselben Wert wie der Synchronisierungspartner gesetzt.
	 *
	 * @return Zeitstempel der letzten Inhaltsänderung. <code>null</code> -->
	 * Zeitstempel unbekannt.
	 */
    @Nullable
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
