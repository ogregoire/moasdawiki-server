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

package net.moasdawiki.service.repository;

import net.moasdawiki.base.ServiceException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Serviceschicht für den Zugriff auf alle Dateien im Wiki-Repository. Das
 * Repository enthält sämtlichen Wikiseiten sowie Bilder, CSS, JavaScript und
 * weitere Binärdateien (z.B. PDF).
 *
 * @author Herbert Reiter
 */
public interface RepositoryService {

    /**
     * Initialisiert den Service. Soll unmittelbar nach dem Konstruktur aufgerufen werden.
     * Wird für die App benötigt.
     */
    void init();

    /**
     * Internen Cache neu aufbauen. Soll nur dann aufgerufen werden, wenn
     * Repository-Dateien außerhalb dieser Klasse geändert wurden, weil diese Operation
     * bei vielen Dateien recht aufwändig sein kann.
     */
    void rebuildCache();

    /**
     * Gibt das AnyFile-Objekt für den angegebenen Dateinamen zurück.
     * <code>null</code> --> Datei nicht vorhanden.
     */
    @Nullable
    AnyFile getFile(@NotNull String filePath);

    /**
     * Listet alle Dateien im Repository auf. Nicht <code>null</code>.
     */
    @NotNull
    Set<AnyFile> getFiles();

    /**
     * Listet alle Dateien auf, deren Inhalt nach dem angegebenen Zeitpunkt
     * geändert wurde.
     * <p>
     * Der Änderungs-Zeitstempel einer Datei kann vom Datei-Zeitstempel des Dateisystems abweichen,
     * wenn der Inhalt synchronisiert wurde und so den Zeitstempel des Server übernommen hat.
     * <p>
     * Die Änderungs-Zeitstempel werden aus einer Cachedatei gelesen, um nicht das ganze Repository
     * scannen zu müssen.
     * <p>
     * Es werden nur Repository-Dateien mit vorhandenem Zeitstempel berücksichtigt, da nur
     * diese zur Synchronisierung geeignet sind. Assets-Dateien in Android haben beispielsweise
     * keinen Zeitstempel.
     *
     * @param modifiedAfter Frühester Änderungszeitpunkt (exklusive).
     *                      <code>null</code> --> alle Dateien auflisten.
     * @return Dateiliste. Nicht <code>null</code>.
     */
    @NotNull
    Set<AnyFile> getModifiedAfter(@Nullable Date modifiedAfter);

    /**
     * Returns a list of the latest n modified files, sorted descending by timestamp.
     *
     * @param count  Maximum number of files to be returned.
     * @param filter Filter for files that match the suffix. null -> do not filter.
     */
    @NotNull
    List<AnyFile> getLastModifiedFiles(int count, @NotNull Predicate<AnyFile> filter);

    /**
     * Löscht die angegebene Datei aus dem Repository.
     */
    void deleteFile(@NotNull AnyFile anyFile) throws ServiceException;

    /**
     * Liest eine Textdatei aus dem Repository ein.
     *
     * @param anyFile Datei im Repository. Nicht <code>null</code>.
     * @return Textinhalt der Datei. Nicht <code>null</code>.
     */
    @NotNull
    String readTextFile(@NotNull AnyFile anyFile) throws ServiceException;

    /**
     * Schreibt eine Textdatei ins Repository. Wenn die Datei bereits existiert,
     * wird sie überschrieben.
     *
     * @param anyFile Datei im Repository. Nicht <code>null</code>.
     * @param content Inhalt der Datei. Nicht <code>null</code>.
     * @return Neues Dateiobjekt mit aktualisierten Attributen.
     */
    @NotNull
    AnyFile writeTextFile(@NotNull AnyFile anyFile, @NotNull String content) throws ServiceException;

    /**
     * Liest eine Binärdatei vom Repository ein.
     *
     * @param anyFile Datei im Repository. Nicht <code>null</code>.
     * @return Inhalt der Datei. Nicht <code>null</code>.
     */
    @NotNull
    byte[] readBinaryFile(@NotNull AnyFile anyFile) throws ServiceException;

    /**
     * Schreibt eine Binärdatei ins Repository. Wenn die Datei bereits
     * existiert, wird sie überschrieben.
     *
     * @param anyFile   Datei im Repository. Nicht <code>null</code>.
     * @param content   Inhalt der Datei. Nicht <code>null</code>.
     * @param timestamp Zeitstempel der Datei. Wird bei der Synchronisierung mit
     *                  einem anderen Repository verwendet. <code>null</code> --> aktuelle Systemzeit verwenden.
     * @return Neues Dateiobjekt mit aktualisierten Attributen.
     */
    @NotNull
    AnyFile writeBinaryFile(@NotNull AnyFile anyFile, @NotNull byte[] content, @Nullable Date timestamp) throws ServiceException;
}
