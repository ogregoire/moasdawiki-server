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

package net.moasdawiki.service.wiki.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.wiki.structure.Anchor;
import net.moasdawiki.service.wiki.structure.Bold;
import net.moasdawiki.service.wiki.structure.Code;
import net.moasdawiki.service.wiki.structure.Color;
import net.moasdawiki.service.wiki.structure.DateTime;
import net.moasdawiki.service.wiki.structure.Heading;
import net.moasdawiki.service.wiki.structure.Html;
import net.moasdawiki.service.wiki.structure.Image;
import net.moasdawiki.service.wiki.structure.IncludePage;
import net.moasdawiki.service.wiki.structure.Italic;
import net.moasdawiki.service.wiki.structure.LineBreak;
import net.moasdawiki.service.wiki.structure.LinkExternal;
import net.moasdawiki.service.wiki.structure.LinkLocalFile;
import net.moasdawiki.service.wiki.structure.LinkPage;
import net.moasdawiki.service.wiki.structure.LinkWiki;
import net.moasdawiki.service.wiki.structure.ListChildren;
import net.moasdawiki.service.wiki.structure.ListEditHistory;
import net.moasdawiki.service.wiki.structure.ListPages;
import net.moasdawiki.service.wiki.structure.ListParents;
import net.moasdawiki.service.wiki.structure.ListUnlinkedPages;
import net.moasdawiki.service.wiki.structure.ListViewHistory;
import net.moasdawiki.service.wiki.structure.ListWantedPages;
import net.moasdawiki.service.wiki.structure.Listable.PageNameFormat;
import net.moasdawiki.service.wiki.structure.Monospace;
import net.moasdawiki.service.wiki.structure.Nowiki;
import net.moasdawiki.service.wiki.structure.OrderedListItem;
import net.moasdawiki.service.wiki.structure.PageElement;
import net.moasdawiki.service.wiki.structure.PageElementList;
import net.moasdawiki.service.wiki.structure.PageName;
import net.moasdawiki.service.wiki.structure.PageTimestamp;
import net.moasdawiki.service.wiki.structure.Paragraph;
import net.moasdawiki.service.wiki.structure.Parent;
import net.moasdawiki.service.wiki.structure.SearchInput;
import net.moasdawiki.service.wiki.structure.Separator;
import net.moasdawiki.service.wiki.structure.Small;
import net.moasdawiki.service.wiki.structure.Strikethrough;
import net.moasdawiki.service.wiki.structure.Style;
import net.moasdawiki.service.wiki.structure.Table;
import net.moasdawiki.service.wiki.structure.TableCell;
import net.moasdawiki.service.wiki.structure.TableOfContents;
import net.moasdawiki.service.wiki.structure.Task;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.Underlined;
import net.moasdawiki.service.wiki.structure.UnorderedListItem;
import net.moasdawiki.service.wiki.structure.VerticalSpace;
import net.moasdawiki.service.wiki.structure.WikiTag;
import net.moasdawiki.service.wiki.structure.WikiVersion;
import net.moasdawiki.service.wiki.structure.XmlTag;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Liest eine Wiki-Seite und erzeugt daraus einen Strukturbaum (entspricht einem
 * Parsebaum bei einer expliziten Grammatik).
 * 
 * @author Herbert Reiter
 */
public class WikiParser {

	@NotNull
	private LineReader lineReader;

	private int openTables; // Anzahl gerade offener Tabellen (geschachtelt)

	public WikiParser(@NotNull Reader reader) throws ServiceException {
		try {
			this.lineReader = new LineReader(reader);
		} catch (IOException e) {
			throw new ServiceException("Error initializing LineReader", e);
		}
	}

	/**
	 * Einstiegspunkt des Parsers.
	 */
	@NotNull
	public PageElementList parse() throws ServiceException {
		try {
			return parsePageElementList();
		} catch (IOException e) {
			throw new ServiceException("Error parsing wiki content", e);
		}
	}

	/**
	 * Parst den Inhalt einer ganzen Wiki-Seite. Wird auch für den Inhalt einer
	 * Tabellenzelle aufgerufen.
	 */
	@NotNull
	private PageElementList parsePageElementList() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElementList content = new PageElementList();

		PageElement pageElement;
		do {
			// nächstes Seitenelement parsen
			pageElement = parsePageElement();

			// neues Seitenelement einfügen
			if (pageElement != null) {
				content.add(pageElement);
			}
		} while (pageElement != null);

		int toPos = lineReader.getCharsReadTotal();
		content.setFromToPos(fromPos, toPos);
		return content;
	}

	/**
	 * Parst das nächste PageElement aus dem Wikitext. Kommentare und Leerzeilen
	 * werden verworfen.
	 * 
	 * Wenn das Ende der Wiki-Seite erreicht ist, wird <code>null</code>
	 * zurückgegeben. Dies ist dann der Fall, wenn das Ende der Datei oder der
	 * aktuellen Zelle ("|") einer Tabelle erreicht wird.
	 */
	@Nullable
	private PageElement parsePageElement() throws IOException {
		while (true) {
			// nächste Zeile vorausschauen, nicht konsumieren
			String line = lineReader.getLine();
			int charsReadLine = lineReader.getCharsReadLine();

			if (line == null) {
				return null; // Inhalt ist zu Ende
			}

			if (line.startsWith("//", charsReadLine)) {
				// Kommentar ignorieren und weiter zum nächsten Element
				lineReader.nextLine();
			} else if (line.startsWith("/*", charsReadLine)) {
				// Kommentar ignorieren und weiter zum nächsten Element
				parseCommentMultiLine();
			} else if (line.length() <= charsReadLine) {
				// Leerzeile ignorieren und weiter zum nächsten Element
				lineReader.nextLine();
			} else if (line.startsWith("=", charsReadLine)) {
				return parseHeading();
			} else if (line.startsWith("----", charsReadLine)) {
				return parseSeparator();
			} else if (line.startsWith("[ ]", charsReadLine) || line.startsWith("[!]", charsReadLine) || line.startsWith("[x]", charsReadLine)
					|| line.startsWith("[X]", charsReadLine)) {
				return parseTask();
			} else if (startsWithListItem('*', line, charsReadLine)) {
				return parseUnorderedList();
			} else if (startsWithListItem('#', line, charsReadLine)) {
				return parseOrderedList();
			} else if (line.startsWith("@@", charsReadLine) && line.substring(charsReadLine + 2).trim().length() == 0 || line.startsWith("@@|", charsReadLine)
					&& !line.substring(charsReadLine + 3).contains("@@")) {
				return parseCode();
			} else if (line.startsWith("{|", charsReadLine)) {
				return parseTable();
			} else if (line.startsWith("{{#", charsReadLine)) {
				return parseAnchor();
			} else if (line.startsWith("{{toc}}", charsReadLine)) {
				return parseTableOfContents();
			} else if (line.startsWith("{{parent:", charsReadLine)) {
				return parseParent();
			} else if (line.startsWith("{{includepage:", charsReadLine)) {
				return parseIncludePage();
			} else if (line.startsWith("{{vspace}}", charsReadLine)) {
				return parseVerticalSpace();
			} else if (openTables > 0 && line.startsWith("|", charsReadLine)) {
				// Tabellenzelle und damit ist die innere Wiki-Seite zu Ende
				return null;
			} else {
				return parseParagraph();
			}
		}
	}

	/**
	 * Überprüft, ob aufgrund des Zeilenanfangs ein neues PageElement beginnt
	 * und der bisherige Absatz damit zu Ende ist.
	 */
	private boolean isNewPageElement(@NotNull String line, int offset) {
		return line.startsWith("//", offset) || line.startsWith("/*", offset) || line.startsWith("=", offset) // Überschrift
				|| line.startsWith("{{title}}", offset) // Seitenüberschrift
				|| line.startsWith("----", offset) // Trennlinie
				|| line.startsWith("[ ]", offset) || line.startsWith("[!]", offset) // Tasks
				|| line.startsWith("[x]", offset) || line.startsWith("[X]", offset) || startsWithListItem('*', line, offset) // Aufzählung
				|| startsWithListItem('#', line, offset) // Nummerierung
				|| line.startsWith("@@", offset) && line.substring(offset + 2).trim().length() == 0 // Code
				|| line.startsWith("@@|", offset) && !line.substring(offset + 3).contains("@@") // Code
				|| line.startsWith("{|", offset) // Tabelle
				|| line.startsWith("{{#", offset) // Anker
				|| line.startsWith("{{toc}}", offset) // Inhaltsverzeichnis
				|| line.startsWith("{{parent:", offset) // Vaterseite
				|| line.startsWith("{{includepage:", offset) // Seite einfügen
				|| line.startsWith("{{center}}", offset) // zentierter Absatz
				|| line.startsWith("{{vspace}}", offset) // vertikaler Abstand
				|| line.length() <= offset; // Leerzeile
	}

	/**
	 * Überprüft, ob in der angegebenen Zeile eine Aufzählung beginnt. Das ist
	 * dann der Fall, wenn die Zeile mit einem oder mehreren
	 * aufeinanderfolgenden Zeichen c gefolgt von einem Leerzeichen beginnt.
	 */
	private boolean startsWithListItem(char c, @Nullable String line, int offset) {
		if (line == null || line.length() <= offset) {
			return false;
		}
		if (line.charAt(offset) != c) {
			return false; // das erste Zeichen darf kein Leerzeichen sein
		}

		int i = offset + 1;
		char ch;
		while (i < line.length()) {
			ch = line.charAt(i);
			if (ch == ' ') {
				return true; // ja, es folgt ein Leerzeichen
			} else if (ch != c) {
				return false; // falsches Zeichen
			}

			i++;
		}

		return false; // kein Leerzeichen gefunden
	}

	/**
	 * Liest einen mehrzeiligen Kommentar ein, bis das Kommentarende "* /"
	 * erreicht ist.
	 */
	private void parseCommentMultiLine() throws IOException {
		String line = lineReader.getLine();
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += 2;  // "/*" am Zeilenanfang entfernen

		// Kommentarende suchen
		while (line != null && line.indexOf("*/", charsReadLine) == -1) {
			lineReader.nextLine();
			line = lineReader.getLine();
			charsReadLine = lineReader.getCharsReadLine();
		}

		// letzte Kommentarzeile auch noch konsumieren
		lineReader.nextLine();
	}

	/**
	 * Liest eine Überschrift ein. Überschriften beginnen mit '='. Die Anzahl
	 * der '='-Zeichen bestimmt die Größe der Überschrift, die größte
	 * Überschrift hat nur ein '='. Optional können am Zeilenende nochmal
	 * '='-Zeichen angegeben werden, um mit gängigen Wikis kompatibel zu sein.
	 * Die komplette Überschrift muss in einer Zeile stehen.
	 */
	@Nullable
	private Heading parseHeading() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Ebene der Überschrift bestimmen
		int level = 1;
		while (level < line.length() - charsReadLine && line.charAt(charsReadLine + level) == '=') {
			level++;
		}

		// "=" am Anfang abschneiden
		charsReadLine += level;
		if (line.startsWith(" ", charsReadLine)) {
			charsReadLine++; // ggf. noch ein Leerzeichen abschneiden
		}

		// Text der Überschrift einlesen
		lineReader.setCharsReadLine(charsReadLine);
		PageElementList content = parseInlineList("=", "\n");

		// Rest der Zeile konsumieren
		lineReader.setCharsReadLine(line.length());
		int toPos = lineReader.getCharsReadTotal();

		return new Heading(level, content, fromPos, toPos);
	}

	/**
	 * Liest eine Trennlinie ein. Eine Trennlinie wird durch "----" angegeben.
	 * Weitere Zeichen in der selben Zeile werden ignoriert.
	 */
	@Nullable
	private Separator parseSeparator() {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		lineReader.setCharsReadLine(line.length()); // ganze Zeile konsumieren
		int toPos = lineReader.getCharsReadTotal();
		return new Separator(fromPos, toPos);
	}

	/**
	 * Liest eine Aufgabe (Task) ein.<br>
	 * <br>
	 * Syntax:<br>
	 * <tt>[ ] offene Aufgabe</tt><br>
	 * <tt>[!] wichtige offene Aufgabe</tt><br>
	 * <tt>[X] erledigte Aufgabe</tt><br>
	 * <tt>[ ] 28.02.2016 | Aufgabe mit Termin</tt><br>
	 * <tt>[ ] 28.02.2016 20:00-22:00 | Aufgabe mit Termin inkl. Uhrzeit</tt><br>
	 * <br>
	 * Die Aufgabenbeschreibung ist ein normaler Absatz (Paragraph). Die
	 * Terminangabe ist optional und kann ein beliebiger Text sein.
	 */
	@Nullable
	private Task parseTask() {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Status bestimmen
		Task.State state;
		if (line.startsWith("[!]", charsReadLine)) {
			state = Task.State.OPEN_IMPORTANT;
		} else if (line.startsWith("[x]", charsReadLine) || line.startsWith("[X]", charsReadLine)) {
			state = Task.State.CLOSED;
		} else {
			state = Task.State.OPEN;
		}
		charsReadLine += 3;  // Status am Anfang abschneiden

		// Termin einlesen, falls vorhanden
		String schedule = null; // kein Termin
		int pos = line.indexOf('|', charsReadLine);
		if (pos >= 0) {
			schedule = line.substring(charsReadLine, pos).trim();
			charsReadLine = pos + 1;
		}

		// Taskbeschreibung einlesen
		if (line.startsWith(" ", charsReadLine)) {
			// ggf. noch ein Leerzeichen abschneiden
			charsReadLine++;
		}
		String description = line.substring(charsReadLine);
		lineReader.setCharsReadLine(line.length()); // Zeilenende
		int toPos = lineReader.getCharsReadTotal();

		return new Task(state, schedule, description, fromPos, toPos);
	}

	/**
	 * Liest einen Aufzählungspunkt ein. Aufzählungen werden durch '*' am
	 * Zeilenanfang gekennzeichnet. Je mehr '*'-Zeichen, desto tiefer die
	 * Schachtelung.
	 */
	@Nullable
	private UnorderedListItem parseUnorderedList() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Ebene der Aufzählung bestimmen
		int level = 1;
		while (level < line.length() - charsReadLine && line.charAt(charsReadLine + level) == '*') {
			level++;
		}

		// Text einlesen
		charsReadLine += level;  // Präfix abschneiden
		if (line.startsWith(" ", charsReadLine)) {
			// ggf. noch ein Leerzeichen abschneiden
			charsReadLine++;
		}
		lineReader.setCharsReadLine(charsReadLine);

		// Textinhalt lesen
		PageElementList inlineList = parseInlineList();
		int toPos = lineReader.getCharsReadTotal();

		return new UnorderedListItem(level, inlineList, fromPos, toPos);
	}

	/**
	 * Liest einen Nummerierungspunkt ein. Nummerierungen werden durch '#' am
	 * Zeilenanfang gekennzeichnet. Je mehr '#'-Zeichen, desto tiefer die
	 * Schachtelung.
	 */
	@Nullable
	private OrderedListItem parseOrderedList() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Ebene der Nummerierung bestimmen
		int level = 1;
		while (level < line.length() - charsReadLine && line.charAt(charsReadLine + level) == '#') {
			level++;
		}

		// Text einlesen
		charsReadLine += level;  // Präfix abschneiden
		if (line.startsWith(" ", charsReadLine)) {
			// ggf. noch ein Leerzeichen abschneiden
			charsReadLine++;
		}
		lineReader.setCharsReadLine(charsReadLine);

		// Textinhalt lesen
		PageElementList inlineList = parseInlineList();
		int toPos = lineReader.getCharsReadTotal();

		return new OrderedListItem(level, inlineList, fromPos, toPos);
	}

	/**
	 * Liest Programmcode ein. Dieser muss mit @@ - allein in einer Zeile -
	 * umschlossen sein und kann auch Leerzeilen enthalten. Bei öffnenden @@-Tag
	 * sind auch Parameter möglich.
	 */
	@Nullable
	private Code parseCode() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Parameter aus Blockanfang auslesen
		String language = null;
		if (line.startsWith("@@|", charsReadLine)) {
			language = line.substring(charsReadLine + 3).trim(); // Rest der Zeile
			if (language.isEmpty()) {
				language = null; // keine Syntaxhervorhebung
			}
		}
		lineReader.nextLine(); // Zeile mit "@@" konsumieren

		// Block bis zum Ende einlesen
		line = lineReader.getLine();
		charsReadLine = lineReader.getCharsReadLine();
		StringBuilder s = new StringBuilder();
		boolean firstLine = true;
		while (line != null) {
			// Wenn Ende-Tag gefunden, aufhören
			if (line.startsWith("@@", charsReadLine) && line.substring(charsReadLine + 2).trim().length() == 0) {
				// komplette Zeile inkl. "@@" konsumieren
				lineReader.nextLine();
				break; // Schleife verlassen
			}

			// alle anderen Zeilen sind als Programmcode zu behandeln
			if (!firstLine) {
				s.append('\n');
			}
			s.append(line.substring(charsReadLine));
			firstLine = false;

			lineReader.nextLine();
			line = lineReader.getLine();
			charsReadLine = lineReader.getCharsReadLine();
		}
		int toPos = lineReader.getCharsReadTotal();

		return new Code(language, s.toString(), fromPos, toPos);
	}

	/**
	 * Liest eine Tabelle ein. Eine Tabelle beginnt mit '{|', ggf. gefolgt von
	 * Tabellenoptionen im HTML-Format, und endet mit '|}'. Zellen beginnen mit
	 * '|' und eine neue Tabellenzeile mit '|-'.
	 * 
	 * Eine Zeile, die mit '||' beginnt, wird als Headerzeile interpretiert.
	 */
	@Nullable
	private Table parseTable() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		openTables++;
		int charsReadLine = lineReader.getCharsReadLine();

		// Parameter aus Tabellenanfang auslesen
		String params = line.substring(charsReadLine + 2).trim(); // Rest der Zeile = Parameter
		if (params.length() == 0) {
			params = null; // keine Formatierung
		}
		lineReader.nextLine(); // aktuelle Zeile abschließen

		Table table = new Table(params, null, null);
		boolean rowCreated = false; // wurde eine entsprechende Zeile erzeugt?

		// Zellen einlesen
		line = lineReader.getLine();
		charsReadLine = lineReader.getCharsReadLine();
		while (line != null) {
			// nächstes Trennzeichen '|' oder '||' oder '|-' oder '|}' suchen
			// und konsumieren
			int pos = line.indexOf('|', charsReadLine);

			if (pos >= 0 && line.startsWith("|-", pos)) {
				// Tabellenzeile abschließen, damit die nächsten Zellen in eine
				// neue Zeile kommen
				// aber noch keine neue Zeile erzeugen, damit es keine leeren
				// Zeilen geben kann
				rowCreated = false;
				charsReadLine += 2; // Zeichen konsumieren

			} else if (pos >= 0 && line.startsWith("|}", pos)) {
				// Tabellenende erreicht
				charsReadLine += 2; // Zeichen konsumieren
				lineReader.setCharsReadLine(charsReadLine);
				break; // keine weiteren Zellen mehr lesen

			} else if (pos >= 0) { // '|' oder '||' gefunden
				// Zelle einlesen (ganze Wiki-Seite)
				boolean isHeaderCell = line.startsWith("||", pos);
				if (isHeaderCell) {
					charsReadLine = pos + 2; // '||' konsumieren
				} else {
					charsReadLine = pos + 1; // '|' konsumieren
				}
				if (line.startsWith(" ", charsReadLine)) {
					charsReadLine++; // ggf. ein Leerzeichen ignorieren
				}
				lineReader.setCharsReadLine(charsReadLine);
				PageElementList content = parsePageElementList();
				if (!rowCreated) {
					table.newRow(null);
					rowCreated = true;
				}
				table.addCell(new TableCell(content, isHeaderCell, null));
				// neuen Zeileninhalt lesen für nächsten Schleifendurchlauf
				line = lineReader.getLine();
				charsReadLine = lineReader.getCharsReadLine();

			} else {
				// kein Trennzeichen gefunden, nächste Zeile einlesen
				lineReader.nextLine();
				line = lineReader.getLine();
				charsReadLine = lineReader.getCharsReadLine();
			}
		}

		openTables--;

		// Tabelle zurückgeben
		int toPos = lineReader.getCharsReadTotal();
		table.setFromToPos(fromPos, toPos);
		return table;
	}

	@Nullable
	private Anchor parseAnchor() {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		int endPos = line.indexOf("}}", charsReadLine);
		String name;
		if (endPos >= 0) {
			// "{{#" vorne abschneiden, "}}" hinten abschneiden
			name = line.substring(charsReadLine + 3, endPos).trim();
			charsReadLine = endPos + 2;
		} else {
			// "{{#" vorne abschneiden, schließende Klammern fehlen
			name = line.substring(charsReadLine + 3).trim();
			charsReadLine = line.length();
		}

		lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
		int toPos = lineReader.getCharsReadTotal();
		return new Anchor(name, fromPos, toPos);
	}

	@NotNull
	private TableOfContents parseTableOfContents() {
		int fromPos = lineReader.getCharsReadTotal();
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += 7; // "{{toc}}" lesen
		lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
		int toPos = lineReader.getCharsReadTotal();
		return new TableOfContents(fromPos, toPos);
	}

	@Nullable
	private Parent parseParent() {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Seitenname extrahieren
		charsReadLine += 9; // "{{parent:" abschneiden
		String parentPagePath;
		int endPos = line.indexOf("}}", charsReadLine);
		if (endPos >= 0) {
			parentPagePath = line.substring(charsReadLine, endPos).trim();
			charsReadLine = endPos + 2;
		} else {
			parentPagePath = line.substring(charsReadLine).trim(); // schließende Klammern fehlen
			charsReadLine = line.length();
		}

		lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
		int toPos = lineReader.getCharsReadTotal();
		return new Parent(parentPagePath, fromPos, toPos);
	}

	@Nullable
	private IncludePage parseIncludePage() {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		charsReadLine += 14; // "{{includepage:" abschneiden
		String parentPagePath;
		int endPos = line.indexOf("}}", charsReadLine);
		if (endPos >= 0) {
			parentPagePath = line.substring(charsReadLine, endPos).trim();
			charsReadLine = endPos + 2;
		} else {
			parentPagePath = line.substring(charsReadLine).trim(); // schließende Klammern fehlen
			charsReadLine = line.length();
		}

		lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
		int toPos = lineReader.getCharsReadTotal();
		return new IncludePage(parentPagePath, fromPos, toPos);
	}

	@NotNull
	private VerticalSpace parseVerticalSpace() {
		int fromPos = lineReader.getCharsReadTotal();
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += 10; // "{{vspace}}" lesen
		lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
		int toPos = lineReader.getCharsReadTotal();
		return new VerticalSpace(fromPos, toPos);
	}

	/**
	 * Liest einen Absatz ein. Ein Absatz kann über mehrere Zeilen gehen und
	 * endet, sobald das nächste PageElement beginnt oder die aktuelle
	 * Tabellenzelle zu Ende ist oder der Wiki-Text zu Ende ist.<br>
	 * <br>
	 * Leerzeichen am Anfang der ersten Zeile bestimmen den linken Einzug. Bei
	 * der Angabe von "{{center}}" am Anfang der ersten Zeile wird der Absatz
	 * zentiert.
	 */
	@Nullable
	private PageElement parseParagraph() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// Zentrierung gesetzt?
		boolean centered = line.startsWith("{{center}}", charsReadLine);
		int indention = 0;
		if (centered) {
			charsReadLine += 10; // center-Tag abschneiden
		} else {
			// Einzug bestimmen
			while (indention < line.length() - charsReadLine && line.charAt(charsReadLine + indention) == ' ') {
				indention++;
			}
			charsReadLine += indention; // Präfix abschneiden
		}
		lineReader.setCharsReadLine(charsReadLine);

		// Text einlesen
		PageElementList content = parseInlineList();
		int toPos = lineReader.getCharsReadTotal();

		return new Paragraph(centered, indention, true, content, fromPos, toPos);
	}

	/**
	 * Liest eine InlineList ein. Diese kann über mehrere Zeilen gehen.<br>
	 * <br>
	 * Eine InlineList endet sobald das nächste PageElement beginnt oder die
	 * aktuelle Tabellenzelle oder der Wiki-Text zu Ende ist oder eine der
	 * angegebenen terminatingTags-Zeichenfolgen erreicht wird.<br>
	 * <br>
	 * Die Terminierung wird nicht konsumiert, das muss die aufrufende
	 * Codestelle erledigen (wg. der korrekten Bestimmung von toPos).
	 * 
	 * @param terminatingTags Liste von Zeichenfolgen, bei denen die InlineList
	 *        zu beenden ist.<br>
	 *        "\n" -> Zeilenende<br>
	 *        "\n\n" -> Leerzeile<br>
	 *        null -> kein vorzeitiges Ende.
	 * @return InlineList, nicht null.
	 */
	@NotNull
	private PageElementList parseInlineList(String... terminatingTags) throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElementList inlineList = new PageElementList();

		PageElement inlineElement;
		do {
			inlineElement = parseInlineElement(terminatingTags);
			if (inlineElement != null) {
				inlineList.add(inlineElement);
			}
		} while (inlineElement != null);

		int toPos = lineReader.getCharsReadTotal();
		inlineList.setFromToPos(fromPos, toPos);
		return inlineList;
	}

	/**
	 * Liest das nächste InlineElement ein. Ist das Ende einer InlineList
	 * erreicht, wird <code>null</code> zurückgegeben.<br>
	 * <br>
	 * Eine InlineList endet sobald das nächste PageElement beginnt oder die
	 * aktuelle Tabellenzelle oder der Wiki-Text zu Ende ist oder eine der
	 * angegebenen terminatingTags-Zeichenfolgen erreicht wird.<br>
	 * <br>
	 * Die Terminierung wird nicht konsumiert, das muss die aufrufende
	 * Codestelle erledigen (wg. der korrekten Bestimmung von toPos).
	 * 
	 * @param terminatingTags Menge von Zeichenfolgen, bei denen die InlineList
	 *        zu beenden ist. Nicht <code>null</code>.<br>
	 *        "\n" -> Zeilenende<br>
	 *        "\n\n" -> Leerzeile<br>
	 *        sonstiger String -> indexOf-Suche<br>
	 *        leer -> kein vorzeitiges Ende.
	 * @return InlineElement. <code>null</code> -> kein weiteres InlineElement.
	 */
	@Nullable
	private PageElement parseInlineElement(String... terminatingTags) throws IOException {
		// nächste Zeile vorausschauen, nicht konsumieren
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		if (openTables > 0 && line.startsWith("|", charsReadLine)) {
			// Tabellenzelle zu Ende
			return null;
		}
		// vorzeitiges Ende durch terminatingTagSet prüfen
		for (String tag : terminatingTags) {
			if ("\n".equals(tag)) {
				if (line.length() <= charsReadLine) {
					return null; // Zeilenwechsel erreicht
				}
			} else if ("\n\n".equals(tag)) {
				if (line.length() <= charsReadLine && charsReadLine == 0) {
					return null; // Leerzeile erreicht
				}
			} else {
				if (line.startsWith(tag, charsReadLine)) {
					return null; // String gefunden
				}
			}
		}
		// nächstes PageElement beginnt
		if (charsReadLine == 0 && isNewPageElement(line, charsReadLine)) {
			return null;
		}

		// nächstes InlineElement einlesen
		if (line.startsWith("''", charsReadLine)) {
			return parseBold();
		} else if (line.startsWith("##", charsReadLine)) {
			// kursiv ("//" geht nicht wg. http://...)
			return parseItalic();
		} else if (line.startsWith("__", charsReadLine)) {
			return parseUnderlined();
		} else if (line.startsWith("~~", charsReadLine)) {
			return parseStrikethrough();
		} else if (line.startsWith("@@", charsReadLine)) {
			return parseMonospace();
		} else if (line.startsWith("°°", charsReadLine)) {
			return parseSmall();
		} else if (line.startsWith("%%", charsReadLine)) {
			return parseNowiki();
		} else if (line.startsWith("[[", charsReadLine)) {
			return parseLink();
		} else if (line.startsWith("{{", charsReadLine)) {
			return parseWikiTag();
		} else if (line.startsWith("<", charsReadLine) && !line.startsWith("< ", charsReadLine)) {
			return parseXmlTag();
		} else if (line.length() <= charsReadLine) {
			// möglicher Zeilenumbruch
			return parseLineBreak();
		} else {
			return parseTextOnly(terminatingTags); // Text
		}
	}

	/**
	 * Liest ein PageElement ein, das zwischen zwei <code>surroundingTag</code>
	 * eingegrenzt ist. Die umschließeden Tags werden ebenfalls konsumiert, der
	 * Rückgabewert ist das eingeschlossene PageElement.
	 * 
	 * @param surroundingTag Umschließende Tags. Nicht <code>null</code>, nicht
	 *        leer.
	 * @return Inneres PageElement.
	 * @throws IOException Wenn ein Fehler auftritt.
	 */
	@NotNull
	private PageElement parseSimpleSurroundedInlineElement(String surroundingTag) throws IOException {
		//noinspection UnusedAssignment
		String line = lineReader.getLine();
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += surroundingTag.length();
		lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen

		// Inhalt lesen
		PageElement content = parseInlineList(surroundingTag);

		// Tagende konsumieren, falls vorhanden
		line = lineReader.getLine();
		charsReadLine = lineReader.getCharsReadLine();
		if (line != null && line.startsWith(surroundingTag, charsReadLine)) {
			charsReadLine += surroundingTag.length();
			lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
		}
		return content;
	}

	@NotNull
	private Bold parseBold() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElement content = parseSimpleSurroundedInlineElement("''");
		int toPos = lineReader.getCharsReadTotal();
		return new Bold(content, fromPos, toPos);
	}

	@NotNull
	private Italic parseItalic() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElement content = parseSimpleSurroundedInlineElement("##");
		int toPos = lineReader.getCharsReadTotal();
		return new Italic(content, fromPos, toPos);
	}

	@NotNull
	private Underlined parseUnderlined() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElement content = parseSimpleSurroundedInlineElement("__");
		int toPos = lineReader.getCharsReadTotal();
		return new Underlined(content, fromPos, toPos);
	}

	@NotNull
	private Strikethrough parseStrikethrough() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElement content = parseSimpleSurroundedInlineElement("~~");
		int toPos = lineReader.getCharsReadTotal();
		return new Strikethrough(content, fromPos, toPos);
	}

	@NotNull
	private Monospace parseMonospace() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElement content = parseSimpleSurroundedInlineElement("@@");
		int toPos = lineReader.getCharsReadTotal();
		return new Monospace(content, fromPos, toPos);
	}

	@NotNull
	private Small parseSmall() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		PageElement content = parseSimpleSurroundedInlineElement("°°");
		int toPos = lineReader.getCharsReadTotal();
		return new Small(content, fromPos, toPos);
	}

	/**
	 * Liest Text ein, der nicht als Wiki-Text interpretiert werden soll. Diese
	 * Funktion setzt die Überwachung des Absatzendes außer Kraft und liest
	 * solange, bis "%%" gelesen wird. Damit ist es möglich, Zeichen mit
	 * Sonderbedeutung (z.B. |, *) auszugeben.<br>
	 * <br>
	 * Um die Zeichenfolge "%%" auszugeben, kann das Wiki-Tag
	 * <code>{{%%}}</code> verwendet werden.
	 */
	@NotNull
	private Nowiki parseNowiki() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += 2; // "%%" abschneiden

		StringBuilder s = new StringBuilder();
		while (line != null) {
			int pos = line.indexOf("%%", charsReadLine);
			if (pos >= 0) {
				// Nowiki-Abschnitt ist zu Ende
				s.append(line, charsReadLine, pos);
				charsReadLine = pos + 2; // "%%" mit abschneiden
				lineReader.setCharsReadLine(charsReadLine);
				break; // while-Schleife verlassen
			} else {
				// ganze Zeile inkl. Zeilenwechsel übernehmen
				s.append(line.substring(charsReadLine));
				s.append('\n');
				lineReader.nextLine();
				line = lineReader.getLine();
				charsReadLine = lineReader.getCharsReadLine();
			}
		}

		int toPos = lineReader.getCharsReadTotal();
		return new Nowiki(s.toString(), fromPos, toPos);
	}

	/**
	 * Liest einen Link ein.<br>
	 * <br>
	 * Links der Form "wiki:..." werden als LinkWiki übersetzt, Links mit einem
	 * anderen Präfix werden als LinkExternal übersetzt; der Präfix ist dann das
	 * Internet-Protokoll (z.B. http). Links ohne Präfix sind Verweise auf
	 * Wiki-Seiten und werden als LinkPage übersetzt. Links mit "file:..." sind
	 * Verweise auf lokale Dateien. Links mit "...@..." werden automatisch als
	 * E-Mail-Links erkannt. Links mit "...#..." und ohne Präfix sind Ankerlinks
	 * auf eine Wiki-Seite.
	 */
	@Nullable
	private PageElement parseLink() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += 2; // "[[" abschneiden

		// Name inkl. Präfix bestimmen
		String pagePath;
		int endpos = line.indexOf("]]", charsReadLine);
		int pipepos = line.indexOf('|', charsReadLine);
		if (pipepos >= 0 && (pipepos < endpos || endpos == -1)) {
			// Format "[[...|...]]"
			// oder "[[...|..." jeweils mit Alternativtext
			pagePath = line.substring(charsReadLine, pipepos).trim();
			charsReadLine = pipepos + 1; // "|" mit abschneiden
			// ggf. erstes Leerzeichen im Alternativtext abschneiden, da dieses
			// optional ist
			if (line.length() > charsReadLine && line.charAt(charsReadLine) == ' ') {
				charsReadLine++;
			}
		} else if (endpos >= 0) {
			// Format "[[...]]" ohne Alternativtext
			pagePath = line.substring(charsReadLine, endpos).trim();
			charsReadLine = endpos + 2; // "]]" abschneiden
		} else {
			// Format "[[..." ohne Alternativtext
			// bis zum Zeilenende als Link-URL verwenden, "]]" ist nicht
			// vorhanden
			pagePath = line.substring(charsReadLine).trim();
			charsReadLine = line.length();
		}
		lineReader.setCharsReadLine(charsReadLine);

		// Präfix und Seitenname trennen
		String prefix;
		int prefixpos = pagePath.indexOf(':');
		if (prefixpos >= 0) {
			// vorderer Teil
			prefix = pagePath.substring(0, prefixpos).trim();
			// hinterer Teil
			pagePath = pagePath.substring(prefixpos + 1).trim();
		} else {
			prefix = null; // kein Präfix vorhanden
		}

		// ggf. alternativen Link-Text einlesen
		PageElementList alternativeText;
		if (pipepos >= 0 && (pipepos < endpos || endpos == -1)) {
			alternativeText = parseInlineList("]]");

			// Tagende konsumieren, falls vorhanden
			line = lineReader.getLine();
			charsReadLine = lineReader.getCharsReadLine();
			if (line != null && line.startsWith("]]", charsReadLine)) {
				charsReadLine += 2;
				lineReader.setCharsReadLine(charsReadLine); // Rest der Zeile übriglassen
			}
		} else {
			alternativeText = null;
		}
		int toPos = lineReader.getCharsReadTotal();

		// Link-Objekt erstellen
		if (prefix != null) {
			if ("wiki".equals(prefix)) {
				return new LinkWiki(pagePath, alternativeText, fromPos, toPos);
			} else if ("file".equals(prefix)) {
				return new LinkLocalFile(pagePath, alternativeText, fromPos, toPos);
			} else {
				return new LinkExternal(prefix + ':' + pagePath, alternativeText, fromPos, toPos);
			}
		} else if (pagePath.indexOf('@') > 0) {
			return new LinkExternal("mailto:" + pagePath, alternativeText, fromPos, toPos);
		} else if (pagePath.indexOf('#') >= 0) { // Anker-Link
			int sharppos = pagePath.indexOf('#');
			if (sharppos > 0) {
				return new LinkPage(pagePath.substring(0, sharppos), pagePath.substring(sharppos + 1), alternativeText, fromPos, toPos);
			} else {
				return new LinkPage(null, pagePath.substring(1), alternativeText, fromPos, toPos);
			}
		} else {
			return new LinkPage(pagePath, null, alternativeText, fromPos, toPos);
		}
	}

	/**
	 * Liest ein WikiTag ein. Dieses hat die Form "{{...}}".
	 */
	@Nullable
	private PageElement parseWikiTag() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		WikiTagDetails tagDetails = parseOpeningWikiTag();
		if (tagDetails == null) {
			return null;
		}
		int toPos = lineReader.getCharsReadTotal();

		switch (tagDetails.tagname) {
			case "br":
				// manueller Zeilenumbruch
				return new LineBreak(fromPos, toPos);
			case "%%":
				// "%%" als Text ausgeben
				return new TextOnly("%%", fromPos, toPos);
			case "image":
				// Bild-Referenz
				if (tagDetails.value == null) {
					return null;
				}
				return new Image(tagDetails.value, tagDetails.options, fromPos, toPos);
			case "html":
				// HTML-Text
				return parseHtml(fromPos);
			case "color":
				// farbiger Text
				return parseColor(tagDetails.value, fromPos);
			case "style":
				// mit CSS-Klassen formatiert
				return parseStyle(tagDetails.value, fromPos);
			case "version":
				return new WikiVersion(fromPos, toPos);
			case "datetime":
				if (tagDetails.options.containsKey("date")) {
					return new DateTime(DateTime.Format.SHOW_DATE, fromPos, toPos);
				} else if (tagDetails.options.containsKey("time")) {
					return new DateTime(DateTime.Format.SHOW_TIME, fromPos, toPos);
				} else {
					return new DateTime(DateTime.Format.SHOW_DATETIME, fromPos, toPos);
				}
			case "pagename":
				return new PageName(extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("link"),
						tagDetails.options.containsKey("globalContext"), fromPos, toPos);
			case "pagetimestamp":
				return new PageTimestamp(tagDetails.options.containsKey("globalContext"), fromPos, toPos);
			case "listviewhistory": {
				int length = -1; // -1 = unbegrenzt

				try {
					String lengthStr = tagDetails.options.get("length");
					if (lengthStr != null) {
						length = Integer.parseInt(lengthStr);
					}
				} catch (NumberFormatException ignored) {
				}
				return new ListViewHistory(extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"),
						tagDetails.options.get("separator"), tagDetails.options.get("outputOnEmpty"), length, fromPos, toPos);
			}
			case "listedithistory": {
				int length = -1; // -1 = unbegrenzt

				try {
					String lengthStr = tagDetails.options.get("length");
					if (lengthStr != null) {
						length = Integer.parseInt(lengthStr);
					}
				} catch (NumberFormatException ignored) {
				}
				return new ListEditHistory(extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"),
						tagDetails.options.get("separator"), tagDetails.options.get("outputOnEmpty"), length, fromPos, toPos);
			}
			case "listparents":
				return new ListParents(tagDetails.value, extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"),
						tagDetails.options.get("separator"), tagDetails.options.get("outputOnEmpty"), tagDetails.options.containsKey("globalContext"), fromPos,
						toPos);
			case "listchildren":
				return new ListChildren(tagDetails.value, extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"),
						tagDetails.options.get("separator"), tagDetails.options.get("outputOnEmpty"), tagDetails.options.containsKey("globalContext"), fromPos,
						toPos);
			case "listpages":
				return new ListPages(tagDetails.value, extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"),
						tagDetails.options.get("separator"), tagDetails.options.get("outputOnEmpty"), tagDetails.options.containsKey("globalContext"), fromPos,
						toPos);
			case "listwantedpages":
				return new ListWantedPages(extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"),
						tagDetails.options.get("separator"), tagDetails.options.get("outputOnEmpty"), fromPos, toPos);
			case "listunlinkedpages":
				return new ListUnlinkedPages(tagDetails.options.containsKey("hideParents"), tagDetails.options.containsKey("hideChildren"),
						extractPageNameFormat(tagDetails.options), tagDetails.options.containsKey("showinline"), tagDetails.options.get("separator"),
						tagDetails.options.get("outputOnEmpty"), fromPos, toPos);
			case "search":
				return new SearchInput(fromPos, toPos);
			default:
				return new WikiTag(tagDetails.tagname, tagDetails.value, tagDetails.options, fromPos, toPos);
		}
	}

	@NotNull
	private static PageNameFormat extractPageNameFormat(@NotNull Map<String, String> options) {
		if (options.containsKey("showPath")) {
			return PageNameFormat.PAGE_PATH;
		} else if (options.containsKey("showFolder")) {
			return PageNameFormat.PAGE_FOLDER;
		} else {
			return PageNameFormat.PAGE_TITLE;
		}
	}

	/**
	 * Liest ein einzelnes Wiki-Tag samt Parameter ein.<br>
	 * <br>
	 * Format-Beispiele:
	 * <ul>
	 * <li>{{tagname}}</li>
	 * <li>{{tagname:wert}}</li>
	 * <li>{{tagname | param1=wert1 | param2="wert 2" | param3 = "wert ""3""" |
	 * param4 }} (param4 wird wie param4="" behandelt)</li>
	 * </ul>
	 */
	@Nullable
	private WikiTagDetails parseOpeningWikiTag() {
		// Name einlesen
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine += 2; // "{{" abschneiden
		int pos = line.length();
		int tmp = line.indexOf("}}", charsReadLine);
		if (tmp > 0 && tmp < pos) {
			pos = tmp;
		}
		tmp = line.indexOf('|', charsReadLine);
		if (tmp > 0 && tmp < pos) {
			pos = tmp;
		}
		String tagname;
		// pos >= 0
		tagname = line.substring(charsReadLine, pos).trim();
		charsReadLine = pos;
		lineReader.setCharsReadLine(charsReadLine);

		// ggf. Wert abtrennen
		String tagvalue = null;
		pos = tagname.indexOf(':');
		if (pos >= 0) {
			tagvalue = tagname.substring(pos + 1);
			tagname = tagname.substring(0, pos);
		}

		// Parameter einlesen
		Map<String, String> options = new HashMap<>();
		//noinspection StatementWithEmptyBody
		while (parseTagParameter(options, true)) {
		}

		// abschließendes "}}" einlesen
		line = lineReader.getLine();
		charsReadLine = lineReader.getCharsReadLine();
		pos = line.indexOf("}}", charsReadLine);
		if (pos >= 0) {
			charsReadLine = pos + 2;
		} else {
			charsReadLine = line.length();
		}
		lineReader.setCharsReadLine(charsReadLine);

		return new WikiTagDetails(tagname, tagvalue, options);
	}

	/**
	 * Liest Text ein, der als HTML-Code interpretiert werden soll. Diese
	 * Funktion setzt die Überwachung des Absatz- und Seitenendes außer Kraft
	 * und liest solange, bis <code>{{/html}}</code> gelesen wird. Damit ist es
	 * möglich, Zeichen mit Sonderbedeutung (z.B. |, *) auszugeben.
	 * 
	 * @param fromPos Gibt die tatsächliche Anfangsposition an, weil das
	 *        öffnende Tag bereits konsumiert wurde.
	 */
	@Nullable
	private Html parseHtml(int fromPos) throws IOException {
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();
		StringBuilder s = new StringBuilder();
		while (line != null) {
			int pos = line.indexOf("{{/html}}", charsReadLine);
			if (pos >= 0) {
				// HTML-Abschnitt ist zu Ende
				if (s.length() > 0) {
					s.append('\n'); // Zeilenwechsel davor
				}
				//noinspection StringOperationCanBeSimplified
				s.append(line.substring(charsReadLine, pos));
				charsReadLine = pos + 9; // "{{/html}}" mit abschneiden
				lineReader.setCharsReadLine(charsReadLine);
				break; // while-Schleife verlassen
			} else {
				// ganze Zeile übernehmen
				if (s.length() > 0) {
					s.append('\n');
				}
				s.append(line.substring(charsReadLine));
				lineReader.nextLine();
				line = lineReader.getLine();
				charsReadLine = lineReader.getCharsReadLine();
			}
		}
		int toPos = lineReader.getCharsReadTotal();

		return new Html(s.toString(), fromPos, toPos);
	}

	/**
	 * Liest den Inhalt eines Color-Tags ein.
	 * 
	 * @param fromPos Gibt die tatsächliche Anfangsposition an, weil das
	 *        öffnende Tag bereits konsumiert wurde.
	 */
	@Nullable
	private Color parseColor(@Nullable String colorName, int fromPos) throws IOException {
		PageElementList content = parseInlineList("{{/color}}");

		// abschließendes "{{/color}}" konsumieren
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();
		if (line.startsWith("{{/color}}", charsReadLine)) {
			charsReadLine += 10;
			lineReader.setCharsReadLine(charsReadLine);
		}
		int toPos = lineReader.getCharsReadTotal();

		if (colorName == null) {
			// erst hier abbrechen, damit das schließende Tag noch konsumiert wird
			return null;
		}
		return new Color(colorName, content, fromPos, toPos);
	}

	/**
	 * Liest den Inhalt eines Style-Tags ein.
	 * 
	 * @param cssNamesStr CSS-Klassen, mit Leerzeichen getrennt.
	 *        <code>null</code> --> keine CSS-Klassen angegeben.
	 * @param fromPos Gibt die tatsächliche Anfangsposition an, weil das
	 *        öffnende Tag bereits konsumiert wurde.
	 */
	private Style parseStyle(@Nullable String cssNamesStr, int fromPos) throws IOException {
		PageElementList content = parseInlineList("{{/style}}");

		// CSS-Angabe parsen
		String[] cssNames = StringUtils.EMPTY_STRING_ARRAY;
		if (cssNamesStr != null) {
			cssNames = StringUtils.splitByWhitespace(cssNamesStr);
		}

		// abschließendes "{{/style}}" konsumieren
		String line = lineReader.getLine();
		int charsReadLine = lineReader.getCharsReadLine();
		if (line != null && line.startsWith("{{/style}}", charsReadLine)) {
			charsReadLine += 10;
			lineReader.setCharsReadLine(charsReadLine);
		}
		int toPos = lineReader.getCharsReadTotal();

		return new Style(cssNames, content, fromPos, toPos);
	}

	/**
	 * Liest ein XML-Tag ein.
	 * 
	 * Ein XmlTag hat stets ein schließendes Tag und umschließt so einen
	 * Textbereich. Abkürzend kann das Tag auch direkt geschlossen werden, wenn
	 * es keinen Inhalt hat, z.B. &lt;tag /&gt;.
	 */
	@Nullable
	private XmlTag parseXmlTag() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		XmlTagDetails tagDetails = parseOpeningXmlTag();
		if (tagDetails == null) {
			return null;
		}

		PageElementList content;
		if (!tagDetails.tagClosed) {
			// eingeschlossenen Inhalt einlesen
			String tagname = tagDetails.tagname;
			if (tagDetails.prefix != null) {
				tagname = tagDetails.prefix + ':' + tagname;
			}
			String closeTag = "</" + tagname + ">";
			content = parseInlineList(closeTag);

			// schließendes Tag konsumieren
			String line = lineReader.getLine();
			int charsReadLine = lineReader.getCharsReadLine();
			if (line != null && line.startsWith(closeTag, charsReadLine)) {
				charsReadLine += closeTag.length();
				lineReader.setCharsReadLine(charsReadLine);
			}
		} else {
			// kein Inhalt, Tag schließt direkt
			content = new PageElementList();
		}
		int toPos = lineReader.getCharsReadTotal();

		return new XmlTag(tagDetails.prefix, tagDetails.tagname, tagDetails.options, content, fromPos, toPos);
	}

	/**
	 * Liest ein einzelnes XML-Tag samt Parameter ein.<br>
	 * <br>
	 * Format-Beispiele:
	 * <ul>
	 * <li>&lt;tagname&gt;</li>
	 * <li>&lt;tagname /&gt;</li>
	 * <li>&lt;präfix:tagname&gt;</li>
	 * <li>&lt;tagname | param1=wert1 | param2="wert 2" | param3 = "wert ""3"""
	 * | param4 /&gt; (param4 wird wie param4="" behandelt)</li>
	 * </ul>
	 */
	@Nullable
	private XmlTagDetails parseOpeningXmlTag() {
		// Name einlesen
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();
		charsReadLine++; // "<" abschneiden
		int pos = line.length();
		int tmp = line.indexOf('>', charsReadLine);
		if (tmp > 0 && tmp < pos) {
			pos = tmp;
		}
		tmp = line.indexOf("/>", charsReadLine);
		if (tmp > 0 && tmp < pos) {
			pos = tmp;
		}
		tmp = line.indexOf(' ', charsReadLine);
		if (tmp > 0 && tmp < pos) {
			pos = tmp;
		}
		String tag;
		tag = line.substring(charsReadLine, pos);
		charsReadLine = pos;
		lineReader.setCharsReadLine(charsReadLine);

		// Name und Präfix trennen
		String tagprefix;
		String tagname;
		pos = tag.indexOf(':');
		if (pos >= 0) {
			tagprefix = tag.substring(0, pos).trim();
			tagname = tag.substring(pos + 1).trim();
		} else {
			tagprefix = null;
			tagname = tag.trim();
		}

		// Parameter einlesen
		Map<String, String> options = new HashMap<>();
		//noinspection StatementWithEmptyBody
		while (parseTagParameter(options, false)) {
		}

		// abschließendes ">" bzw. "/>" einlesen
		line = lineReader.getLine();
		charsReadLine = lineReader.getCharsReadLine();
		pos = line.indexOf('>', charsReadLine);
		boolean tagClosed = (pos > 0 && line.charAt(pos - 1) == '/');
		if (pos >= 0) {
			charsReadLine = pos + 1;
		} else {
			charsReadLine = line.length();
		}
		lineReader.setCharsReadLine(charsReadLine);

		return new XmlTagDetails(tagprefix, tagname, options, tagClosed);
	}

	/**
	 * Liest ein Wertepaar Name=Wert ein. Ist nur ein Name vorhanden, wird er
	 * wie Name="" behandelt.
	 *
	 * @return Parameter gefunden?
	 */
	private boolean parseTagParameter(@NotNull Map<String, String> options, boolean isWikiTag) {
		if (isWikiTag) {
			String line = lineReader.getLine();
			if (line == null) {
				return false;
			}
			int charsReadLine = lineReader.getCharsReadLine();
			// nächstes '|' suchen und konsumieren
			int pos = line.indexOf('|', charsReadLine);
			if (pos == -1 || line.substring(charsReadLine, pos).trim().length() > 0) {
				return false;
			}
			charsReadLine = pos + 1;
			lineReader.setCharsReadLine(charsReadLine);
		}

		// Parameter-Name lesen
		String name = parseParameterWord();

		// Parameter-Wert lesen
		String value;
		String line = lineReader.getLine();
		int charsReadLine = lineReader.getCharsReadLine();
		if (line != null && line.length() > charsReadLine && line.charAt(charsReadLine) == '=') {
			charsReadLine++;
			lineReader.setCharsReadLine(charsReadLine);
			value = parseParameterWord();
		} else {
			value = ""; // kein Wert
		}

		// Wertepaar in Liste eintragen
		if (name != null && name.length() > 0) {
			options.put(name, value);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Liest ein Wort ein. Das Wort kann auch in Anführungszeichen stehen.
	 * Leerzeichen am Anfang und Ende werden auch konsumiert, aber ignoriert.
	 * 
	 * @return eingelesenes Wort, ist nie null
	 */
	@Nullable
	private String parseParameterWord() {
		final String endChars = " =|/>]}";
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();

		// führende Leerzeichen konsumieren
		while (charsReadLine < line.length() && line.charAt(charsReadLine) == ' ') {
			charsReadLine++;
		}

		// Wort einlesen
		String word;
		if (line.length() > charsReadLine && line.charAt(charsReadLine) == '\"') {
			// in Anführungszeichen gesetzt
			charsReadLine++; // Anführungszeichen konsumieren
			StringBuilder s = new StringBuilder();
			while (charsReadLine < line.length()) {
				char c = line.charAt(charsReadLine);
				if (c == '\"' && (charsReadLine + 1 == line.length() || line.charAt(charsReadLine + 1) != '\"')) {
					break; // schließendes Anführungszeichen gefunden
				}
				s.append(c);
				if (c == '\"') {
					charsReadLine++; // doppeltes Anführungszeichen konsumieren
				}
				charsReadLine++;
			}
			word = s.toString(); // kein trim(), da in Anführungszeichen!
			if (charsReadLine < line.length()) {
				// schließendes Anführungszeichen konsumieren
				charsReadLine++;
			} else {
				charsReadLine = line.length();
			}
		} else {
			// nicht in Anführungszeichen gesetzt, bis zum nächsten Endezeichen
			// lesen
			int pos = charsReadLine;
			while (pos < line.length() && endChars.indexOf(line.charAt(pos)) == -1) {
				pos++;
			}
			word = line.substring(charsReadLine, pos).trim();
			charsReadLine = pos; // konsumieren
		}

		// anschließende Leerzeichen konsumieren
		while (charsReadLine < line.length() && line.charAt(charsReadLine) == ' ') {
			charsReadLine++;
		}

		lineReader.setCharsReadLine(charsReadLine);
		return word;
	}

	/**
	 * Liest einen Zeilenumbruch ein und erzeugt daraus ggf. einen LineBreak.
	 * Ein LineBreak wird dann erzeugt, wenn die InlineList in der nächsten
	 * Zeile nicht zu Ende ist.
	 */
	@Nullable
	private LineBreak parseLineBreak() throws IOException {
		int fromPos = lineReader.getCharsReadTotal();
		lineReader.nextLine();

		// nächste Zeile vorausschauen, nicht konsumieren
		String line = lineReader.getLine();
		int charsReadLine = lineReader.getCharsReadLine();

		if (line == null) {
			return null; // Ende der Seite erreicht
		}

		if (isNewPageElement(line, charsReadLine)) {
			return null; // nächstes Seitenelement beginnt
		}

		if (openTables != 0 && line.startsWith("|", charsReadLine)) {
			return null; // nächste Tabellenzelle beginnt
		}
		int toPos = lineReader.getCharsReadTotal();

		return new LineBreak(fromPos, toPos);
	}

	/**
	 * Liest einen einzeiligen Text ein. Der Text endet spätestens am Zeilenende
	 * oder das nächste InlineElement beginnt oder die aktuelle Tabellenzelle
	 * oder die angegebene terminatingTag-Zeichenfolge erreicht wird.
	 * 
	 * @param terminatingTags Menge von Zeichenfolgen, bei denen die InlineList
	 *        zu beenden ist. Nicht <code>null</code>.
	 */
	@Nullable
	private TextOnly parseTextOnly(@NotNull String... terminatingTags) {
		int fromPos = lineReader.getCharsReadTotal();
		String line = lineReader.getLine();
		if (line == null) {
			return null;
		}
		int charsReadLine = lineReader.getCharsReadLine();
		// Hinweis: gemäß parseInlineElement() ist line nicht leer

		// Position, ab der ein nachfolgendes InlineElement beginnt oder Zeilenende
		int endpos = charsReadLine;
		whileLoop: while (endpos < line.length()) {
			char c = line.charAt(endpos);
			if (openTables > 0 && c == '|') {
				// Tabellenzelle zu Ende?
				break;
			}
			// vorzeitiges Ende erreicht?
			for (String tag : terminatingTags) {
				if (line.startsWith(tag, endpos)) {
					break whileLoop;
				}
			}
			if (endpos + 1 < line.length()) {
				char c2 = line.charAt(endpos + 1);
				if ((c == '\'' && c2 == '\'')
						|| (c == '#' && c2 == '#')
						|| (c == '_' && c2 == '_')
						|| (c == '~' && c2 == '~')
						|| (c == '°' && c2 == '°')
						|| (c == '@' && c2 == '@')
						|| (c == '%' && c2 == '%')
						|| (c == '{' && c2 == '{')
						|| (c == '[' && c2 == '[')
						|| (c == '<' && c2 != ' ')) {
					break;
				}
			}
			endpos++;
		}

		String text = line.substring(charsReadLine, endpos);
		charsReadLine = endpos;
		lineReader.setCharsReadLine(charsReadLine);
		int toPos = lineReader.getCharsReadTotal();

		return new TextOnly(text, fromPos, toPos);
	}

	/**
	 * Hilfsklasse für parseOpeningXmlTag().
	 */
	private static class XmlTagDetails {
		// null -> kein Präfix
		@Nullable
		private final String prefix;

		@NotNull
		private final String tagname;

		@NotNull
		private final Map<String, String> options;

		// Tag in Kurznotation geschlossen?
		private final boolean tagClosed;

		public XmlTagDetails(@Nullable String prefix, @NotNull String tagname, @NotNull Map<String, String> options, boolean tagClosed) {
			this.prefix = prefix;
			this.tagname = tagname;
			this.options = options;
			this.tagClosed = tagClosed;
		}
	}

	/**
	 * Hilfsklasse für parseOpeningWikiTag().
	 */
	private static class WikiTagDetails {

		/**
		 * Name des Wiki tags.
		 */
		@NotNull
		private final String tagname;

		/**
		 * Wert nach Doppelpunkt ':', sonst null.
		 */
		@Nullable
		private final String value;

		/**
		 * Optionale Parameter
		 */
		@NotNull
		private final Map<String, String> options;

		public WikiTagDetails(@NotNull String tagname, @Nullable String value, @NotNull Map<String, String> options) {
			this.tagname = tagname;
			this.value = value;
			this.options = options;
		}
	}
}
