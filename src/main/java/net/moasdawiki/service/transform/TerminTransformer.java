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

package net.moasdawiki.service.transform;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.PageElementConsumer;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.DateUtils;
import net.moasdawiki.util.PathUtils;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Durchsucht das ganze Repository nach Kontakten mit Geburtstag und Terminen.
 * Diese werden in einer Tabelle aufgelistet. Bei Datumsangaben werden - soweit
 * sinnvoll interpretierbar - auch Teilangaben mit fehlendem Tag, Monat oder
 * Jahr unterstützt und der Termin am Monats- bzw. Jahresanfang angezeigt.<br>
 * <br>
 * <b>Syntax:</b>
 * <ul>
 * <li><tt>&lt;terminliste /&gt;</tt> &nbsp; Aktuelle Geburtstage Termine, 3
 * Tage in der Vergangenheit bis 10 Tage in der Zukunft.</li>
 * <li><tt>&lt;terminliste tagedanach="0" tagedavor="7" /&gt;</tt> &nbsp;
 * Aktuelle Geburtstage und Termine, von heute bis 7 Tage in der Zukunft.</li>
 * <li><tt>&lt;terminliste jahr="2016" /&gt;</tt> &nbsp; Jahresübersicht</li>
 * </ul>
 */
public class TerminTransformer implements TransformWikiPage {

	private static final String TABLE_AGE_KEY = "TerminTransformer.table.age";
	private static final String TABLE_DATE_KEY = "TerminTransformer.table.date";
	private static final String TABLE_NAME_KEY = "TerminTransformer.table.name";

	private static final int DEFAULT_TAGE_DANACH = 3;
	private static final int DEFAULT_TAGE_DAVOR = 10;

	private static final String EVENTS_CACHE_FILEPATH = "/events.cache";

	private final Logger logger;
	private final Messages messages;
	private final RepositoryService repositoryService;
	private final WikiService wikiService;

	/**
	 * Is repository scanning allowed to update the cache content?
	 * Is set to false for the App as the cache file is updates by synchronization.
	 */
	private final boolean scanRepository;

	/**
	 * Cache aller Geburtstage und Termine, um nicht bei jeder Auflistung alle
	 * Wikiseite erneut scannen zu müssen.
	 */
	@NotNull
	private List<Event> eventCache;

	/**
	 * Zeitpunkt der letzten Cache-Aktualisierung. Dadurch kann der Caches nach
	 * einer Synchronisierung effizient aktualisiert werden.
	 * <code>null</code> --> noch nie.
	 */
	@Nullable
	private Date cacheTimestamp;

	/**
	 * Konstruktor.
	 */
	public TerminTransformer(@NotNull Logger logger, @NotNull Messages messages,
							 @NotNull RepositoryService repositoryService,
							 @NotNull WikiService wikiService, boolean scanRepository) {
		this.wikiService = wikiService;
		this.logger = logger;
		this.messages = messages;
		this.repositoryService = repositoryService;
		this.scanRepository = scanRepository;
		this.eventCache = new ArrayList<>();
		reset();
	}

	/**
	 * Rereads the cache file.
	 * Is called in App environment after synchronization with server.
	 */
	public void reset() {
		if (!readEventsFromCacheFile()) {
			updateCache();
		}
	}

	/**
	 * Liest die Cachedatei mit den Events ein und aktualisiert die Einträge im
	 * Cache.<br>
	 * <br>
	 * Dateiformat der Cachedatei:
	 * <ul>
	 * <li>Die erste Zeile enthält den Zeitstempel der letzten
	 * Cacheaktualisierung, also den Stand der Datei.</li>
	 * <li>In den darauffolgenden Zeilen wird pro Zeile ein Event beschrieben.
	 * </li>
	 * <li>Spaltentrenner ist das Tabulatorzeichen.</li>
	 * <li>Spalte 1 enthält den Dateipfad der Wikidatei.</li>
	 * <li>Spalte 2 enthält das Datum des Events.</li>
	 * <li>Spalte 3 enthält die Beschreibung des Events (optional).</li>
	 * </ul>
	 */
	private boolean readEventsFromCacheFile() {
		try {
			AnyFile anyFile = new AnyFile(EVENTS_CACHE_FILEPATH);
			String cacheContent = repositoryService.readTextFile(anyFile);
			CacheFile cacheFile = parseCacheFile(cacheContent);
			cacheTimestamp = cacheFile.timestamp;
			eventCache = cacheFile.eventList;
			logger.write(cacheFile.eventList.size() + " events read from cache file");
			return true;
		} catch (ServiceException e) {
			logger.write("Error reading events cache file");
			return false;
		}
	}

	/**
	 * Parst die Cachedatei.
	 */
	@NotNull
	private CacheFile parseCacheFile(@NotNull String cacheContent) throws ServiceException {
		try {
			CacheFile result = new CacheFile();
			BufferedReader reader = new BufferedReader(new StringReader(cacheContent));

			// Zeitstempel in erster Zeile
			String timestampStr = reader.readLine();
			result.timestamp = DateUtils.parseUtcDate(timestampStr);
			if (result.timestamp == null) {
				throw new ServiceException("Cache timestamp missing in first row");
			}

			// Parent-Mappings in nachfolgenden Zeilen
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					// Leerzeile (am Ende) ignorieren
					continue;
				}
				int pos1 = line.indexOf('\t');
				int pos2 = line.indexOf('\t', pos1 + 1);
				if (pos1 < 0 || pos2 < 0) {
					throw new ServiceException("Invalid cache file format");
				}

				// Wikiseite
				String wikiFilePath = line.substring(0, pos1);

				// Datum
				String dateStr = line.substring(pos1 + 1, pos2);
				DateFields dateFields = parseGermanDate(dateStr);

				// Beschreibung (optional)
				String description = StringUtils.emptyToNull(line.substring(pos2 + 1));

				Event event = new Event();
				event.pagePath = wikiFilePath;
				event.description = description;
				event.dateFields = dateFields;
				result.eventList.add(event);
			}
			reader.close();
			return result;
		} catch (IOException e) {
			throw new ServiceException("Error parsing cache file", e);
		}
	}

	@NotNull
	public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
		return TransformerHelper.transformPageElements(wikiPage, this::transformPageElement);
	}

	@NotNull
	private PageElement transformPageElement(@NotNull PageElement pageElement) {
		if (pageElement instanceof XmlTag) {
			XmlTag xmlTag = (XmlTag) pageElement;

			if (xmlTag.getPrefix() == null && "terminliste".equalsIgnoreCase(xmlTag.getName())) {
				String jahr = xmlTag.getOptions().get("jahr");
				String tagedanach = xmlTag.getOptions().get("tagedanach");
				String tagedavor = xmlTag.getOptions().get("tagedavor");

				if (jahr != null) {
					// Jahresübersicht erzeugen
					try {
						int jahrInt = Integer.parseInt(jahr);
						return generateYearList(jahrInt);
					} catch (NumberFormatException e) {
						// falsche Syntax, unverändert lassen
						return pageElement;
					}

				} else {
					// nur aktuellen Ausschnitt erzeugen
					try {
						int tagedanachInt = DEFAULT_TAGE_DANACH;
						if (tagedanach != null) {
							tagedanachInt = Integer.parseInt(tagedanach);
						}
						int tagedavorInt = DEFAULT_TAGE_DAVOR;
						if (tagedavor != null) {
							tagedavorInt = Integer.parseInt(tagedavor);
						}
						return generateCurrentDaysList(tagedanachInt, tagedavorInt);
					} catch (NumberFormatException e) {
						// falsche Syntax, unverändert lassen
						return pageElement;
					}
				}
			} else {
				// kein Terminliste-Tag, unverändert lassen
				return pageElement;
			}
		} else {
			// kein XML-Tag, unverändert lassen
			return pageElement;
		}
	}

	/**
	 * Return all events from the internal cache.
	 * Is called by the CalendarSyncAdapter in the App environment.
	 */
	@SuppressWarnings("unused")
	@NotNull
	public List<Event> getEvents() {
		return Collections.unmodifiableList(eventCache);
	}

	/**
	 * Update the cache content by scanning the repository for changes.
	 */
	private void updateCache() {
		if (!scanRepository) {
			return;
		}

		Set<String> modifiedWikiFilePaths = wikiService.getModifiedAfter(cacheTimestamp);
		if (modifiedWikiFilePaths.isEmpty()) {
			// no changes in repository, cache is still up to date
			return;
		}
		logger.write("Scanning " + modifiedWikiFilePaths.size() + " new wiki files for events");

		// scan changed files
		cacheTimestamp = new Date();
		for (String wikiFilePath : modifiedWikiFilePaths) {
			removeEvents(wikiFilePath, eventCache);
			readBirthday(wikiFilePath, eventCache);
			readTasks(wikiFilePath, eventCache);
		}

		// remove events from deleted files
		eventCache.removeIf(event -> !wikiService.existsWikiFile(event.pagePath));

		writeEventsToCacheFile();
		logger.write("Event cache updated, contains " + eventCache.size() + " events");
	}

	/**
	 * Write all events to the cache file.
	 */
	private void writeEventsToCacheFile() {
		StringBuilder sb = new StringBuilder();

		// write timestamp to first line
		String timestampStr = DateUtils.formatUtcDate(cacheTimestamp);
		sb.append(timestampStr).append('\n');

		// write events
		for (Event event : eventCache) {
			sb.append(event.pagePath);
			sb.append('\t');
			sb.append(formatGermanDate(event.dateFields));
			sb.append('\t');
			if (event.description != null) {
				sb.append(event.description);
			}
			sb.append('\n');
		}
		String cacheContent = sb.toString();

		// save cache file
		try {
			AnyFile anyFile = new AnyFile(EVENTS_CACHE_FILEPATH);
			repositoryService.writeTextFile(anyFile, cacheContent);
		} catch (ServiceException e) {
			logger.write("Error writing cache file " + EVENTS_CACHE_FILEPATH, e);
		}
	}

	/**
	 * Erzeugt eine Tabelle mit allen Geburtstagen und Terminen in einem Jahr
	 * und berechnet das Alter zum angegebenen Jahr.
	 * 
	 * @param jahr Jahr zu dem das Alter berechnet werden soll.
	 * @return Tabelle mit der Terminliste. Nicht <code>null</code>.
	 */
	private PageElement generateYearList(int jahr) {
		updateCache();

		List<ResultEntry> entryList = new ArrayList<>();
		for (Event event : eventCache) {
			// Alter berechnen, wenn das Geburtsjahr bekannt ist
			Integer age = null;
			if (event.dateFields.year != null) {
				age = jahr - event.dateFields.year;
			}

			// keine Termine für künftige Jahre anzeigen
			if (age == null || age >= 0) {
				ResultEntry entry = new ResultEntry();
				entry.pagePath = event.pagePath;
				entry.description = event.description;
				entry.dateFields = new DateFields();
				entry.dateFields.day = event.dateFields.day;
				entry.dateFields.month = event.dateFields.month;
				entry.dateFields.year = jahr; // aktuelles Jahr anzeigen
				entry.sortDateFields = entry.dateFields;
				entry.age = age;
				entryList.add(entry);
			}
		}

		return generateTable(entryList);
	}

	/**
	 * Erzeugt eine Tabelle mit den aktuellen Geburtstagen und Terminen (bezogen
	 * auf das Serverdatum) und berechnet das aktuelle Alter.
	 * 
	 * @param tagedanach Listenanfang, Tage nach dem Termin.
	 * @param tagedavor Listenende, Tage vor dem Termin.
	 * @return Tabelle mit der Terminliste.
	 */
	@NotNull
	private PageElement generateCurrentDaysList(int tagedanach, int tagedavor) {
		updateCache();

		Calendar todayCal = Calendar.getInstance();
		DateFields today = new DateFields(todayCal);

		Calendar fromCal = (Calendar) todayCal.clone();
		fromCal.add(Calendar.DAY_OF_YEAR, -tagedanach);
		DateFields from = new DateFields(fromCal);

		Calendar toCal = (Calendar) todayCal.clone();
		toCal.add(Calendar.DAY_OF_YEAR, tagedavor);
		DateFields to = new DateFields(toCal);

		List<ResultEntry> entryList = new ArrayList<>();
		for (Event event : eventCache) {
			// Liegt der Termin im gesuchten Tage-Intervall?
			// [heute - tagedavor, heute + tagedanach]
			DateFields date1 = new DateFields();
			date1.day = event.dateFields.day;
			date1.month = event.dateFields.month;
			date1.year = from.year;

			DateFields date2 = new DateFields();
			date2.day = date1.day;
			date2.month = date1.month;
			date2.year = to.year;

			DateFields sortDate;
			if (date1.compareTo(from) >= 0 && date1.compareTo(to) <= 0) {
				// Termin liegt noch im alten Jahr (nur relevant an
				// Jahresgrenze)
				sortDate = date1;
			} else if (date2.compareTo(from) >= 0 && date2.compareTo(to) <= 0) {
				// Termin liegt schon im neuen Jahr (nur relevant an
				// Jahresgrenze)
				sortDate = date2;
			} else {
				continue; // außerhalb des Intervalls
			}

			// Alter berechnen, wenn das Geburtsjahr bekannt ist
			Integer age = null;
			if (event.dateFields.year != null) {
				age = sortDate.year - event.dateFields.year;
			}
			// keine Termine aus zukünftigen Jahren anzeigen
			if (age != null && age < 0) {
				continue;
			}

			ResultEntry entry = new ResultEntry();
			entry.pagePath = event.pagePath;
			entry.description = event.description;
			entry.dateFields = event.dateFields;
			entry.sortDateFields = sortDate;
			entry.tense = getTense(sortDate.compareTo(today));
			entry.age = age;
			entryList.add(entry);
		}

		return generateTable(entryList);
	}

	/**
	 * Übersetzt das Ergebnis eines Datumsvergleichs in einen
	 * {@link TerminTransformer.ResultEntry.Tense}-Wert.
	 * 
	 * @param compareResult Ergebnis des Datumsvergleichs.
	 * @return Tense-Wert.
	 */
	@NotNull
	private static ResultEntry.Tense getTense(int compareResult) {
		if (compareResult < 0) {
			return ResultEntry.Tense.PAST;
		} else if (compareResult == 0) {
			return ResultEntry.Tense.PRESENT;
		} else {
			return ResultEntry.Tense.FUTURE;
		}
	}

	/**
	 * Löscht alle Events zur angegebenen Wikiseite. Wird aufgerufen, bevor eine
	 * Wikiseite neu gescannt wird.
	 */
	private void removeEvents(@NotNull String wikiFilePath, @NotNull List<Event> eventList) {
		eventList.removeIf(event -> wikiFilePath.equals(event.pagePath));
	}

	/**
	 * Durchsucht die angegebene Wikiseite nach einem Geburtstag. Kontakte mit
	 * fehlenden oder ungültigen Angaben werden ignoriert.
	 */
	private void readBirthday(@NotNull String wikiFilePath, @NotNull List<Event> eventList) {
		WikiFile wikiFile;
		try {
			wikiFile = wikiService.getWikiFile(wikiFilePath);
		}
		catch (ServiceException e) {
			logger.write("Error reading wiki page to scan for events, ignoring it");
			return;
		}

		BirthdayData birthdayData = new BirthdayData();
		PageElementConsumer<XmlTag, BirthdayData> consumer = (xmlTag, context) -> {
			if (xmlTag.getPrefix() == null && "kontakt".equals(xmlTag.getName())) {
				context.kontaktTagFound = true;
			}

			if (context.kontaktTagFound && xmlTag.getPrefix() == null && ("geburtstag".equals(xmlTag.getName()) || "geburtsdatum".equals(xmlTag.getName()))) {
				context.birthday = KontaktseiteTransformer.getStringContent(xmlTag);
			}

			if (context.kontaktTagFound && xmlTag.getPrefix() == null && "todestag".equals(xmlTag.getName())) {
				context.hasDayOfDeath = true;
			}
		};
		WikiHelper.traversePageElements(wikiFile.getWikiPage(), consumer, XmlTag.class, birthdayData, true);

		if (birthdayData.birthday != null && !birthdayData.hasDayOfDeath) {
			DateFields dateFields = parseGermanDate(birthdayData.birthday);
			// zumindest Monat oder Jahr müssen angegeben sein
			if (dateFields != null && (dateFields.month != null || dateFields.year != null)) {
				Event event = new Event();
				event.pagePath = wikiFile.getWikiFilePath();
				event.dateFields = dateFields;
				eventList.add(event);
			}
		}
	}

	private static class BirthdayData {
		public boolean kontaktTagFound;
		public String birthday;
		public boolean hasDayOfDeath;
	}

	/**
	 * Durchsucht die angegebene Wikiseite nach offenen Aufgaben mit Terminen.
	 * Aufgaben mit fehlenden oder ungültigen Angaben werden ignoriert.
	 */
	private void readTasks(@NotNull String wikiFilePath, @NotNull List<Event> eventList) {
		WikiFile wikiFile;
		try {
			wikiFile = wikiService.getWikiFile(wikiFilePath);
		}
		catch (ServiceException e) {
			logger.write("Error reading wiki file to scan it for tasks, ignoring it");
			return;
		}

		// Aufgaben suchen
		List<Task> taskList = new ArrayList<>();
		WikiHelper.traversePageElements(wikiFile.getWikiPage(), (task, context) -> context.add(task), Task.class, taskList, true);

		// Aufgaben mit Termin in Event umwandeln
		for (Task task : taskList) {
			if (task.getState() == Task.State.CLOSED) {
				continue; // Aufgabe ignorieren
			}

			DateFields dateFields = parseGermanDate(task.getSchedule());
			// zumindest Monat oder Jahr müssen angegeben sein
			if (dateFields != null && (dateFields.month != null || dateFields.year != null)) {
				Event event = new Event();
				event.pagePath = wikiFile.getWikiFilePath();
				event.description = task.getDescription();
				event.dateFields = dateFields;
				eventList.add(event);
			}
		}
	}

	/**
	 * Erzeugt eine chronologisch sortierte Tabelle aus der angegebenen Liste.
	 * 
	 * @param entryList Liste der Kontakte.
	 * @return Die Tabelle. Nicht <code>null</code>.
	 */
	@NotNull
	private PageElement generateTable(@NotNull List<ResultEntry> entryList) {
		// Liste chronologisch sortieren
		Collections.sort(entryList);

		Table table = new Table("geburtstagsliste", null, null);
		// Überschriften
		TableRow row = new TableRow(null);
		table.addRow(row);
		String dateStr = messages.getMessage(TABLE_DATE_KEY);
		row.addCell(new TableCell(new TextOnly(dateStr), true, null));
		String nameStr = messages.getMessage(TABLE_NAME_KEY);
		row.addCell(new TableCell(new TextOnly(nameStr), true, null));
		String ageStr = messages.getMessage(TABLE_AGE_KEY);
		row.addCell(new TableCell(new TextOnly(ageStr), true, "right"));

		// Termine
		for (ResultEntry entry : entryList) {
			if (entry.tense != null) {
				row = new TableRow(entry.tense.name().toLowerCase());
			} else {
				row = new TableRow(null);
			}
			table.addRow(row);

			// Datum
			String params = null;
			if (entry.dateFields.year != null) {
				params = "right"; // rechtsbündig ausrichten
			}
			row.addCell(new TableCell(new TextOnly(formatGermanDate(entry.dateFields)), false, params));
			
			// Linktext
			String linktext = entry.description;
			if (linktext == null) {
				linktext = PathUtils.extractWebName(entry.pagePath);
			}
			row.addCell(new TableCell(new LinkPage(entry.pagePath, new TextOnly(linktext)), false, null));
			
			// Alter
			Integer age = entry.age;
			if (age != null) {
				row.addCell(new TableCell(new TextOnly(age.toString()), false, "right"));
			} else {
				row.addCell(new TableCell(null, false, null));
			}
		}
		return table;
	}

	/**
	 * Parst eine Datumsangabe. Folgende Formate werden unterstützt:
	 * "TT.MM.JJJJ", "TT.MM.", "MM.JJJJ", "MM.", "JJJJ".
	 * 
	 * @param dateStr Datumsangabe.
	 * @return Geparstes Datum. null -> kein Wert vorhanden oder ungültiges
	 *         Format.
	 */
	@Nullable
	private static DateFields parseGermanDate(@Nullable String dateStr) {
		if (dateStr == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(dateStr);

		// trim + Rest (z.B. Uhrzeit) abschneiden
		while (sb.charAt(0) == ' ') {
			sb.deleteCharAt(0);
		}
		int spacePos = sb.indexOf(" ");
		if (spacePos > 0) {
			sb.delete(spacePos, sb.length());
		}

		int pos1 = sb.indexOf(".");
		int pos2 = sb.indexOf(".", pos1 + 1);
		DateFields result = new DateFields();

		try {
			// Tag parsen
			if (pos2 >= 0) {
				result.day = Integer.parseInt(sb.substring(0, pos1));
			}

			// Monat parsen
			if (pos2 >= 0) {
				result.month = Integer.parseInt(sb.substring(pos1 + 1, pos2));
			} else if (pos1 >= 0) {
				result.month = Integer.parseInt(sb.substring(0, pos1));
			}

			// Jahr parsen
			if (pos2 >= 0) {
				if (sb.length() > pos2 + 1) {
					result.year = Integer.parseInt(sb.substring(pos2 + 1));
				}
			} else if (pos1 >= 0) {
				if (sb.length() > pos1 + 1) {
					result.year = Integer.parseInt(sb.substring(pos1 + 1));
				}
			} else if (sb.length() > 0) {
				result.year = Integer.parseInt(sb.toString());
			}

			return result;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Formatiert ein Datum im Format "TT.MM.JJJJ". Fehlende Angaben werden
	 * weggelassen.
	 * 
	 * @return Formatierte Textdarstellung. <code>null</code> -> kein Wert
	 *         vorhanden.
	 */
	@NotNull
	public static String formatGermanDate(@Nullable DateFields dateFields) {
		if (dateFields == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		if (dateFields.day != null) {
			if (dateFields.day < 10) {
				sb.append('0');
			}
			sb.append(dateFields.day);
			sb.append('.');
		}

		if (dateFields.month != null) {
			if (dateFields.month < 10) {
				sb.append('0');
			}
			sb.append(dateFields.month);
			sb.append('.');
		}

		if (dateFields.year != null) {
			sb.append(dateFields.year);
		}
		return sb.toString();
	}

	/* ============================ Hilfsklassen ============================ */

	/**
	 * DTO mit den Angaben eines Datums.
	 */
	public static class DateFields {
		public Integer day;
		public Integer month;
		public Integer year;

		/**
		 * Konstruktor.
		 */
		public DateFields() {
			super();
		}

		/**
		 * Konstruktor. Übernimmt die Datumsangabe aus dem angegebenen Calendar.
		 * 
		 * @param cal Calendar-Instanz. Nicht <code>null</code>.
		 */
		public DateFields(Calendar cal) {
			super();
			this.day = cal.get(Calendar.DAY_OF_MONTH);
			this.month = cal.get(Calendar.MONTH) + 1;
			this.year = cal.get(Calendar.YEAR);
		}

		/**
		 * Vergleicht das Objekt mit dem angegebenen Objekt gemäß
		 * {@link Comparable#compareTo(Object)}. Dabei werden fehlende Tages-
		 * und Monatsangaben als 1 behandelt.
		 */
		public int compareTo(@NotNull DateFields date2) {
			// Jahr vergleichen
			if (year != null && date2.year != null && year.intValue() != date2.year.intValue()) {
				return year.compareTo(date2.year);
			}

			return compareToIgnoreYear(date2);
		}

		/**
		 * Vergleicht das Objekt mit dem angegebenen Objekt gemäß
		 * {@link Comparable#compareTo(Object)}. Dabei werden fehlende Tages-
		 * und Monatsangaben als 1 behandelt und die Jahresangabe vollständig
		 * ignoriert.
		 */
		public int compareToIgnoreYear(@NotNull DateFields date2) {
			// Jahr ignorieren

			// Monat vergleichen
			Integer m1 = 1;
			if (month != null) {
				m1 = month;
			}
			Integer m2 = 1;
			if (date2.month != null) {
				m2 = date2.month;
			}
			if (m1.intValue() != m2.intValue()) {
				return m1.compareTo(m2);
			}

			// Tag vergleichen
			Integer d1 = 1;
			if (day != null) {
				d1 = day;
			}
			Integer d2 = 1;
			if (date2.day != null) {
				d2 = date2.day;
			}
			return d1.compareTo(d2);
		}

		@Override
		public String toString() {
			return "(" + day + ", " + month + ", " + year + ")";
		}
	}

	/**
	 * DTO für ein Event.
	 */
	public static class Event {

		/**
		 * Die Wikiseite, die das Event enthält/beschreibt.
		 */
		public String pagePath;

		/**
		 * Beschreibung des Events. <code>null</code> -> stattdessen Name der
		 * Wikiseite verwenden.
		 */
		@Nullable
		public String description;

		/**
		 * Datumsangabe. Nicht <code>null</code>, einige Felder können aber
		 * <code>null</code> sein.
		 */
		public DateFields dateFields;
	}

	/**
	 * DTO für den Eintrag einer Terminliste.
	 */
	private static class ResultEntry implements Comparable<ResultEntry> {

		/**
		 * Die Wikiseite, die das Event enthält/beschreibt.
		 */
		public String pagePath;

		/**
		 * Beschreibung des Events. <code>null</code> -> stattdessen Name der
		 * Wikiseite verwenden.
		 */
		public String description;

		/**
		 * Anzuzeigendes Datum. Nicht <code>null</code>, einige Felder können
		 * aber <code>null</code> sein.
		 */
		public DateFields dateFields;

		/**
		 * Zur Sortierung zu verwendendes Datum. Nicht <code>null</code>, einige
		 * Felder können aber <code>null</code> sein.
		 */
		public DateFields sortDateFields;

		/**
		 * Zeitbereich. <code>null</code> -> nicht verwendet.
		 */
		public Tense tense;

		/**
		 * Alter des Ereignisses in Jahren. <code>null</code> -> unbekannt.
		 */
		public Integer age;

		public int compareTo(@NotNull ResultEntry resultEntry2) {
			return sortDateFields.compareTo(resultEntry2.sortDateFields);
		}

		/**
		 * Aufzählung für vergangene, aktuelle und zukünftige Termine.
		 */
		public enum Tense {
			PAST, PRESENT, FUTURE
		}
	}

	/**
	 * Content of the cache file.
	 */
	private static class CacheFile {
		public Date timestamp;
		public final List<Event> eventList = new ArrayList<>();
	}
}
