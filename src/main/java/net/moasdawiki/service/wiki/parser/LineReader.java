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

package net.moasdawiki.service.wiki.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Zerlegt eine Eingabe in Zeilen. Erlaubt das Zurückschreiben eines Teils einer
 * Zeile.
 */
public class LineReader {

	private static final String CR = "\r";
	private static final String LF = "\n";
	private static final String CRLF = "\r\n";
	private static final String EMPTY = "";

	/**
	 * Reader aus dem die Eingabe gelesen wird.
	 */
	@NotNull
	private final BufferedReader reader;

	/**
	 * Bei der Erkennung der Zeilenenden kann es vorkommen, dass ein Zeichen
	 * zuviel aus dem Reader gelesen wurde. Dann kann es hier zurückgesetellt
	 * werden.
	 *
	 * <code>-1</code> -> kein Zeichen zurückgestellt, nächstes Zeichen muss aus
	 * dem Reader gelesen werden.
	 */
	private int unreadChar;

	/**
	 * Anzahl Zeichen, die insgesamt bereits gelesen wurden. Wird verwendet, um
	 * die Editorpositionen von Seitenelementen zu ermitteln.
	 */
	private int charsReadTotal;

	/**
	 * Zeichen die das Ende der aktuellen Zeile markieren. Kann "\r", "\n", "\r\n" oder "" (bei EOF) sein.
	 *
	 * Wird benötigt, um den Zähler {@link #charsReadTotal} korrekt hochzuzählen.
	 */
	@NotNull
	private String lineEndChars;

	/**
	 * Aktuelle Zeile. <code>null</code> -> Eingabe ist zu Ende (EOF).
	 */
	@Nullable
	private String line;

	/**
	 * Anzahl Zeichen der aktuellen Zeile in {@link #line}, die bereits gelesen wurden.
	 */
	private int charsReadLine;

	/**
	 * Erzeugt eine neue Instanz.
	 * 
	 * @param reader Reader mit den Eingabedaten.
	 * @throws IOException Wenn ein Lesefehler auftritt.
	 */
	public LineReader(@NotNull Reader reader) throws IOException {
		super();
		this.reader = new BufferedReader(reader);
		this.unreadChar = -1;
		this.charsReadTotal = 0;
		this.lineEndChars = EMPTY;
		this.line = EMPTY;
		this.charsReadLine = 0;
		nextLine(); // Zeile initialisieren
	}

	/**
	 * Setzt den Inhalt der nächsten Zeile. Wenn das Ende der Eingabe erreicht
	 * ist, wird <code>null</code> gesetzt.<br>
	 * <br>
	 * Ein Zeilenende wird durch ein Zeichen '\n' oder ein Zeichen '\r' oder die
	 * Zeichenfolge "\r\n" markiert. Die Zeilenende-Markierung ist nicht im
	 * Inhalt der Zeile enthalten.
	 */
	@SuppressWarnings("StringEquality")
	public void nextLine() throws IOException {
		// Rest der zuletzt aktuellen Zeile zählen
		if (line != null) {
			int remainingChars = line.length() - charsReadLine;
			charsReadTotal += remainingChars;
		}
		charsReadTotal += lineEndChars.length();
		lineEndChars = EMPTY;

		StringBuilder lineContent = new StringBuilder();
		boolean eof = true; // Ende der Eingabe erreicht?
		whileLoop: while (true) {
			// nächstes Zeichen lesen
			int ch;
			if (unreadChar >= 0) {
				ch = unreadChar;
				unreadChar = -1;
			} else {
				ch = reader.read();
			}
			if (ch >= 0) {
				eof = false;
			}

			// Zeichen verarbeiten
			switch (ch) {
			case -1:
				break whileLoop; // Eingabe zu Ende
			case '\r':
				lineEndChars = CR;
				break;
			case '\n':
				if (lineEndChars == CR) {
					lineEndChars = CRLF;
				} else {
					lineEndChars = LF;
				}
				break whileLoop; // Zeilenende erreicht
			default:
				if (lineEndChars == CR) {
					// Zeilentrennzeichen ist '\r', es kommt kein '\n' mehr,
					// daher das zuviel gelesene Zeichen zurückstellen
					unreadChar = ch;
					break whileLoop; // Zeilenende erreicht
				} else {
					lineContent.append((char) ch);
				}
			}
		}

		if (eof) {
			line = null; // Ende der Eingabe erreicht
		} else {
			line = lineContent.toString();
		}
		charsReadLine = 0;
	}

	/**
	 * Gibt die aktuelle Zeile zurück. <code>null</code> -> Eingabe ist zu Ende
	 * (EOF).
	 */
	@Nullable
	public String getLine() {
		return line;
	}

	/**
	 * Gibt zurück, wie viele Zeichen insgesamt bereits gelesen wurden.
	 * Entspricht dem Dateizeiger beim Bearbeiten einer Datei.
	 */
	public int getCharsReadTotal() {
		return charsReadTotal;
	}

	/**
	 * Gibt zurück, wie viele Zeichen der aktuellen Zeile bereits gelesen
	 * wurden.
	 * 
	 * @see #getLine()
	 * @see #setCharsReadLine(int)
	 */
	public int getCharsReadLine() {
		return charsReadLine;
	}

	/**
	 * Setzt die Anzahl der Zeichen der aktuellen Zeile, die bereits gelesen wurden.
	 * Wird auch aufgerufen, wenn zuviel gelesene Zeichen wieder als "ungelesen" markiert werden sollen, allerdings nur innerhalb der aktuellen Zeile.
	 */
	public void setCharsReadLine(int numRead) {
		if (line != null && numRead >= 0 && numRead <= line.length()) {
			charsReadTotal += (numRead - charsReadLine);
			charsReadLine = numRead;
		}
	}

	/**
	 * Gibt zurück, ob das Ende der Eingabe erreicht ist, d.h. es gibt keine
	 * weiteren Zeichen zu lesen.
	 * 
	 * @return <code>true</code> -> Ende der Eingabe ist erreicht.
	 */
	public boolean eof() {
		return (line == null);
	}
}
