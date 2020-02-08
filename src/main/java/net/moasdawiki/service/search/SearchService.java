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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.search.SearchResult.Marker;
import net.moasdawiki.service.search.SearchResult.MatchingLine;
import net.moasdawiki.service.search.SearchResult.PageDetails;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.util.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementiert die Volltextsuche im Repository.
 * 
 * @author Herbert Reiter
 */
public class SearchService {

	@NotNull
	private final Logger logger;

	@NotNull
	private final WikiService wikiService;

	/**
	 * Konstruktor.
	 */
	public SearchService(@NotNull Logger logger, @NotNull WikiService wikiService) {
		super();
		this.logger = logger;
		this.wikiService = wikiService;
	}

	/**
	 * Liest die Suchbedingungen aus einem Query-String. Unterstützt werden
	 * folgende Angaben:
	 * 
	 * <ul>
	 * <li>Wort: sucht nach einem Wort</li>
	 * <li>"mehrere Wörter": sucht nach einer Textphrase, die auch Leerzeichen
	 * enthält</li>
	 * <li>-Wort: ein Wort, das nicht vorkommen darf</li>
	 * <li>-"mehrere Wörter": Textphrase mit Leerzeichen, die nicht vorkommen
	 * darf</li>
	 * </ul>
	 * 
	 * @param query Query-String.
	 * @return Suchbedingungen.
	 */
	@SuppressWarnings("ConstantConditions")
	@NotNull
	public SearchQuery parseQueryString(@NotNull String query) {
		Set<String> included = new HashSet<>();
		Set<String> excluded = new HashSet<>();
		int i = 0;
		boolean includedPhrase = true;
		boolean insideQuote = false;
		StringBuilder phrase = new StringBuilder();
		while (query != null && i < query.length()) {
			char ch = query.charAt(i);
			if (phrase.length() == 0 && !insideQuote) {
				// Steuerzeichen interpretieren
				if (ch == '-' && includedPhrase) {
					includedPhrase = false;
				} else if (ch == '\"' && !insideQuote) {
					insideQuote = true;
				} else if (ch == ' ' || ch == '\t') {
					// zurücksetzen
					includedPhrase = true;
					insideQuote = false;
				} else {
					phrase.append(ch);
				}
			} else if (insideQuote && ch == '\"' || !insideQuote && (ch == ' ' || ch == '\t')) {
				// Phrasenende erreicht
				if (includedPhrase) {
					included.add(phrase.toString());
				} else {
					excluded.add(phrase.toString());
				}
				includedPhrase = true;
				insideQuote = false;
				phrase.setLength(0);
			} else {
				// Phrase einlesen
				phrase.append(ch);
				if (i + 1 == query.length()) {
					if (includedPhrase) {
						included.add(phrase.toString());
					} else {
						excluded.add(phrase.toString());
					}
				}
			}
			i++;
		}

		if (phrase.length() > 0) {
			// Phrasenende erreicht
			if (includedPhrase) {
				included.add(phrase.toString());
			} else {
				excluded.add(phrase.toString());
			}
		}

		return new SearchQuery(query, included, excluded);
	}

	/**
	 * Durchsucht alle Wiki-Seiten, die auf die SearchQuery passen.
	 * Die SearchQuery muss dabei mindestens eine zu suchende Textphrase enthalten.
	 * Der Stringvergleich behandelt ähnlich aussehende Zeichen gleich, siehe siehe {@link #generateNormalizedPattern(Set)}.
	 * 
	 * @return Suchergebnis.
	 */
	@NotNull
	public SearchResult searchInRepository(@NotNull SearchQuery searchQuery) throws ServiceException {
		SearchResult searchResult = new SearchResult();
		searchResult.searchQuery = searchQuery;

		if (searchQuery.getIncluded().size() == 0) {
			// kein vorzukommender Suchtext angegeben --> leere Ergebnisliste
			return searchResult;
		}

		// Suchstrings normalisieren und zu einem Pattern kombinieren
		Pattern[] includedPatterns = new Pattern[searchQuery.getIncluded().size()];
		int i = 0;
		for (String findStr : searchQuery.getIncluded()) {
			includedPatterns[i] = generateNormalizedPattern(Collections.singleton(findStr));
			i++;
		}
		Pattern includedPattern = generateNormalizedPattern(searchQuery.getIncluded());
		Pattern excludedPattern = null;
		if (searchQuery.getExcluded().size() >= 1) {
			excludedPattern = generateNormalizedPattern(searchQuery.getExcluded());
		}

		// alle Wikiseiten durchsuchen
		for (String filePath : wikiService.getWikiFilePaths()) {
			WikiFile wikiFile = wikiService.getWikiFile(filePath);
			if (isPageMatching(wikiFile.getWikiFilePath(), wikiFile.getWikiText(), includedPatterns, excludedPattern)) {
				PageDetails pageDetails = scanPage(wikiFile.getWikiFilePath(), wikiFile.getWikiText(), includedPattern);
				searchResult.resultList.add(pageDetails);
			}
		}

		// Treffer nach absteigender Relevanz sortieren
		searchResult.resultList.sort((p1, p2) -> Integer.compare(p2.relevance, p1.relevance));

		return searchResult;
	}

	/**
	 * Überprüft, ob die angegebene Wikiseite den Suchbedingungen entspricht.
	 * Alle einzuschließenden Textphrasen müssen vorkommen, von den
	 * auszuschließenden Textphrasen darf keine einzige vorkommen (soweit welche
	 * angegeben wurden). Der Stringvergleich behandelt ähnlich aussehende
	 * Zeichen gleich, siehe {@link #generateNormalizedPattern(Set)}.
	 * 
	 * @param pagePath Pfad der Wikiseite.
	 * @param wikiText Text der Wikiseite.
	 * @param includedPatterns Zu suchende Textphrasen.
	 * @param excludedPattern Textphrasen, die nicht vorkommen dürfen. <code>null</code> --> nicht relevant.
	 * @return <code>true</code>, wenn die Wikiseite die Suchbedingungen erfüllt.
	 */
	private boolean isPageMatching(@NotNull String pagePath, @NotNull String wikiText, @NotNull Pattern[] includedPatterns, @Nullable Pattern excludedPattern) {
		// Wikiseite normalisieren
		String pagePathNorm = unicodeNormalize(pagePath);
		String wikiTextNorm = unicodeNormalize(wikiText);

		// alle einzuschließenden Textphrasen müssen vorkommen
		for (Pattern pattern : includedPatterns) {
			if (!pattern.matcher(pagePathNorm).find() && !pattern.matcher(wikiTextNorm).find()) {
				// Text nicht gefunden
				return false;
			}
		}

		// es darf keine auszuschließende Textphrase geben
		// Ausschluss-Text gefunden --> Seite ignorieren
		return excludedPattern == null || (!excludedPattern.matcher(pagePathNorm).find() && !excludedPattern.matcher(wikiTextNorm).find());
	}

	/**
	 * Sammelt alle Trefferzeilen in der angegebenen Wikiseite auf.
	 * 
	 * @param pagePath Pfad der Wikiseite.
	 * @param wikiText Text der Wikiseite.
	 * @param includedPattern Zu suchende Textphrasen.
	 * @return Trefferdetails zur Wikiseite. null -> Suchtext nicht enthalten
	 */
	@Nullable
	private PageDetails scanPage(@NotNull String pagePath, @NotNull String wikiText, @NotNull Pattern includedPattern) throws ServiceException {
		PageDetails result = new PageDetails();
		result.pagePath = pagePath;

		// Fundstellen im Seitennamen aufsammeln
		MatchingCategories mc = new MatchingCategories();
		scanPageTitle(pagePath, includedPattern, mc, result);

		// Fundstellen im Seitentext aufsammeln
		scanPageText(pagePath, wikiText, includedPattern, mc, result);

		// Relevanz berechnen
		result.relevance = calculateRelevance(mc);
		if (result.relevance > 0) {
			return result;
		} else {
			return null; // Seite enthält den Suchtext nicht
		}
	}

	private void scanPageTitle(@NotNull String pagePath, @NotNull Pattern includedPattern, @NotNull MatchingCategories mc, @NotNull PageDetails pageDetails) {
		pageDetails.titleLine.line = pagePath;

		// Wikiseite normalisieren
		String pagePathNorm = unicodeNormalize(pagePath);
		String pageName = PathUtils.extractWebName(pagePath);
		String pageNameNorm = unicodeNormalize(pageName);

		Matcher mPath = includedPattern.matcher(pagePathNorm);
		Matcher mName = includedPattern.matcher(pageNameNorm);
		if (mPath.matches()) {
			// kompletter Seitentitel mit Pfadangabe passt
			mc.titleComplete = true;
			mc.titleWord++;
			if (mPath.start() < mPath.end()) {
				pageDetails.titleLine.positions.add(new Marker(mPath.start(), mPath.end()));
			}

		} else if (mName.matches()) {
			// kompletter Seitenname (nach letztem '/') passt
			mc.titleComplete = true;
			mc.titleWord++;
			int prefixLen = pagePath.length() - pageName.length();
			if (mName.start() < mName.end()) {
				pageDetails.titleLine.positions.add(new Marker(prefixLen + mName.start(), prefixLen + mName.end()));
			}

		} else {
			// Substring-Treffer
			mPath.reset();
			while (mPath.find()) {
				if (isWordAligned(pagePathNorm, mPath)) {
					mc.titleWord++;
				} else {
					mc.titleSubstring++;
				}
				if (mPath.start() < mPath.end()) {
					pageDetails.titleLine.positions.add(new Marker(mPath.start(), mPath.end()));
				}
			}
		}
	}

	private void scanPageText(@NotNull String pagePath, @NotNull String wikiText, @NotNull Pattern includedPattern, @NotNull MatchingCategories mc, @NotNull PageDetails pageDetails)
			throws ServiceException {
		try {
			BufferedReader reader = new BufferedReader(new StringReader(wikiText));
			String line = reader.readLine();
			while (line != null) {
				MatchingLine matchingLine = null;
				String lineNorm = unicodeNormalize(line);
				Matcher m = includedPattern.matcher(lineNorm);
				while (m.find()) {
					if (m.start() < m.end()) {
						if (isHeading(lineNorm)) {
							// Treffer in einer Überschrift
							if (isWordAligned(lineNorm, m)) {
								mc.headingWord++;
							} else {
								mc.headingSubstring++;
							}
						} else {
							// Treffer im normalen Text
							if (isWordAligned(lineNorm, m)) {
								mc.paragraphWord++;
							} else {
								mc.paragraphSubstring++;
							}
						}

						if (matchingLine == null) {
							matchingLine = new MatchingLine();
							matchingLine.line = line;
							pageDetails.textLines.add(matchingLine);
						}
						if (m.start() < m.end()) {
							matchingLine.positions.add(new Marker(m.start(), m.end()));
						}
					}
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			// kann eigentlich nicht auftreten
			String message = "Fehler beim Durchsuchen der Wiki-Seite '" + pagePath + "'";
			logger.write(message, e);
			throw new ServiceException(message, e);
		}
	}

	/**
	 * Generiert ein kombiniertes Pattern aus den angegebenen Suchstrings.
	 * Dabei werden ähnlich aussehende Zeichen gleich behandelt und
	 * diakritische Zeichen sowie die Groß-/Kleinschreibung ignoriert,
	 * d.h. ä=ae, ö=oe, ü=ue, ß=ss, a=à=á=â=A=À=Á=Â usw.
	 * 
	 * @param findStrings Suchstrings, darf nicht leer sein
	 * @return Pattern-Objekt.
	 */
	@NotNull
	private Pattern generateNormalizedPattern(@NotNull Set<String> findStrings) throws ServiceException {
		StringBuilder combined = new StringBuilder();
		for (String findStr : findStrings) {
			if (findStr.startsWith("regex:")) {
				// regulären Ausdruck unverändert übernehmen
				findStr = findStr.substring(6);

				// Pattern testweise kompilieren, um Fehler zu melden
				try {
					Pattern.compile(findStr);
				} catch (PatternSyntaxException e) {
					throw new ServiceException("Fehlerhafter regulärer Ausdruck bei der Wiki-Suche: " + findStr, e);
				}

			} else {
				// RegEx-Sonderzeichen unschädlich machen
				findStr = escapeRegEx(findStr);

				// Umlaute und ß/ss als gleich behandeln
				findStr = expandUmlaute(findStr);

				// Unicodezeichen normalisieren
				findStr = unicodeNormalize(findStr);

				// Groß-/Kleinschreibung ignorieren und mit Unicodebehandlung
				findStr = "?iu:" + findStr;
			}

			// an kombinierten Ausdruck anhängen
			if (combined.length() > 0) {
				combined.append('|');
			}
			combined.append('(').append(findStr).append(')');
		}

		// Pattern kompilieren
		try {
			return Pattern.compile(combined.toString());
		} catch (Exception e) {
			throw new ServiceException("Syntax error in regular search expression: " + combined.toString(), e);
		}
	}

	/**
	 * Escaped alle Zeichen, die in einem regulären Ausdruck eine Sonderbedeutung haben.
	 * Im Unterschied zu {@link Pattern#quote(String)} wird der String nicht einfach mit \Q und \E umschlossen,
	 * sondern alle Sonderzeichen einzeln mit '\' escaped,
	 * damit später noch einzelne Zeichen durch Logik ersetzt werden können.
	 *
	 * Hinweis: Das Zeichen '^' wird ignoriert, weil es später durch
	 * {@link #unicodeNormalize(String)} als diakritisches Zeichen gelöscht wird
	 * und dann nur noch das Escapezeichen stehen bleiben würde.
	 */
	@NotNull
	static String escapeRegEx(@NotNull String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch == '\\' || ch == '.' || ch == '-' || ch == '[' || ch == ']' || ch == '(' || ch == ')' || ch == '{' || ch == '}' || ch == '|' || ch == '&'
					|| ch == '$' || ch == '*' || ch == '+' || ch == '?') {
				sb.append('\\');
			}
			sb.append(ch);
		}
		return sb.toString();
	}

	/**
	 * Ergänzt Umlaute und ausgewählte Sonderzeichen durch eine Auswahl mit Alternativen,
	 * so dass ähnlich aussehende Zeichen (ohne diakritische Zeichen) als gleich behandelt werden.
	 */
	@NotNull
	static String expandUmlaute(@NotNull String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			char ch = Character.toLowerCase(str.charAt(i));
			char ch2 = '\0';
			if (i + 1 < str.length()) {
				ch2 = Character.toLowerCase(str.charAt(i + 1));
			}

			if (ch == 'ä' || ch == 'a' && ch2 == 'e') {
				sb.append("(ä|ae)");
				if (ch == 'a') {
					i++;
				}
			} else if (ch == 'ö' || ch == 'o' && ch2 == 'e') {
				sb.append("(ö|oe)");
				if (ch == 'o') {
					i++;
				}
			} else if (ch == 'ü' || ch == 'u' && ch2 == 'e') {
				sb.append("(ü|ue)");
				if (ch == 'u') {
					i++;
				}
			} else if (ch == 'ß' || ch == 's' && ch2 == 's') {
				sb.append("(ß|ss)");
				if (ch == 's') {
					i++;
				}
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Führt eine Unicode-Normalisierung durch und entfernt diakritische Zeichen.
	 * Die Länge des Strings bleibt dabei unverändert.
	 *
	 * Beispiele: Résumé --> Resume, Säure --> Saure
	 */
	@NotNull
	static String unicodeNormalize(@NotNull String str) {
		str = Normalizer.normalize(str, Normalizer.Form.NFKD);
		// "IsLm" und "IsSk" sind unter Android unbekannt, daher "Lm" und "Sk"
		return str.replaceAll("[\\p{InCombiningDiacriticalMarks}\\p{Lm}\\p{Sk}]+", "");
	}

	/**
	 * Gibt zurück, ob sich die aktuelle Fundstelle des Matchers an einer Wortgrenze befindet.
	 * Ansonsten handelt es sich um einen Teilstring-Fund.
	 * 
	 * @param text Text, in dem der Treffer gefunden wurde.
	 * @param matcher Matcher, der auf eine Fundstelle verweist.
	 * @return <code>true</code>, wenn sich die Fundstelle an einer Wortgrenze befindet.
	 */
	private boolean isWordAligned(@NotNull String text, @NotNull Matcher matcher) {
		int start = matcher.start();
		int end = matcher.end();
		return (start == 0 || !Character.isLetter(text.codePointAt(start - 1))) && (end == text.length() || !Character.isLetter(text.codePointAt(end)));
	}

	/**
	 * Gibt zurück, ob die angegebene Textzeile eine Überschrift darstellt,
	 * d.h. z.B. mit "= " beginnt.
	 * 
	 * @param text Textzeile.
	 * @return <code>true</code>, wenn es sich um eine Überschrift handelt.
	 */
	private boolean isHeading(@NotNull String text) {
		return text.startsWith("=");
	}

	/**
	 * Berechnet aus den Trefferanzahlen nach Kategorien eine Relevanzzahl.
	 * Dabei werden folgende Regeln angewandt:
	 * 
	 * <ul>
	 * <li><code>titleComplete</code> zählt mehr als <code>titleWord</code></li>
	 * <li><code>titleWord</code> zählt mehr als <code>headingWord</code></li>
	 * <li><code>headingWord</code> zählt mehr als <code>paragraphWord</code></li>
	 * <li><code>paragraphWord</code> zählt mehr als <code>titleSubstring</code>
	 * </li>
	 * <li><code>titleSubstring</code> zählt mehr als
	 * <code>headingSubstring</code></li>
	 * <li><code>headingSubstring</code> zählt mehr als
	 * <code>paragraphSubstring</code></li>
	 * <li>innerhalb derselben Kategorie zählt eine höhere Trefferanzahl mehr</li>
	 * <li>eine Trefferanzahl > 9 wird wie 9 gewertet</li>
	 * </ul>
	 * 
	 * @param mc Trefferanzahlen nach Kategorien.
	 * @return Relevanzzahl >= <code>0</code>. <code>0</code> = kein Treffer.
	 */
	private int calculateRelevance(@NotNull MatchingCategories mc) {
		int result = 0;
		if (mc.titleComplete) {
			result += 1000000;
		}
		result += 100000 * Math.min(mc.titleWord, 9);
		result += 10000 * Math.min(mc.headingWord, 9);
		result += 1000 * Math.min(mc.paragraphWord, 9);
		result += 100 * Math.min(mc.titleSubstring, 9);
		result += 10 * Math.min(mc.headingSubstring, 9);
		result += Math.min(mc.paragraphSubstring, 9);
		return result;
	}

	/**
	 * DTO mit den Trefferanzahl nach Kategorien.
	 * Diese werden anschließend in eine einzelne Relevanzzahl umgerechnet, um einfacher sortierbar zu sein.
	 *
	 * <code>...Complete</code> = Übereinstimmung mit dem gesamten Absatz<br>
	 * <code>...Word</code> = Übereinstimmung mit Wortgrenzen<br>
	 * <code>...Substring</code> = Übereinstimmung nur als Substring, nicht an Wortgrenzen
	 */
	private static class MatchingCategories {
		public boolean titleComplete;
		public int titleWord;
		public int titleSubstring;

		public int headingWord;
		public int headingSubstring;

		public int paragraphWord;
		public int paragraphSubstring;
	}
}
