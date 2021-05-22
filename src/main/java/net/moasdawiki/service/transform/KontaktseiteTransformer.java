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

package net.moasdawiki.service.transform;

import net.moasdawiki.service.wiki.WikiHelper;
import net.moasdawiki.service.wiki.structure.*;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Erzeugt eine formatierte Ausgabe einen Personen-Kontakts. Angezeigt werden
 * nur aktuelle Daten, d.h. bei denen das Attribut <code>gültigbis</code> nicht
 * gesetzt ist.
 */
public class KontaktseiteTransformer implements TransformWikiPage {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

	@NotNull
	public WikiPage transformWikiPage(@NotNull WikiPage wikiPage) {
		return TransformerHelper.transformPageElements(wikiPage, this::transformPageElement);
	}

	@NotNull
	private PageElement transformPageElement(@NotNull PageElement pageElement) {
		if (pageElement instanceof XmlTag) {
			XmlTag xmlTag = (XmlTag) pageElement;

			if (xmlTag.getPrefix() == null && "kontakt".equals(xmlTag.getName())) {
				Kontakt kontakt = getKontakt(xmlTag);
				return formatKontakt(kontakt);
			} else {
				return pageElement; // kein Kontakt-Tag, unverändert lassen
			}
		} else {
			return pageElement; // kein Kontakt-Tag, unverändert lassen
		}
	}

	/**
	 * Liest einen kompletten Kontakt ein.
	 */
	@SuppressWarnings("StatementWithEmptyBody")
	@NotNull
	private Kontakt getKontakt(@NotNull XmlTag kontaktTag) {
		// XML-Tags auf oberster Ebene suchen
		List<XmlTag> xmlTagList = new ArrayList<>();
		if (kontaktTag.getChild() != null) {
			WikiHelper.traversePageElements(kontaktTag.getChild(), (xmlTag, context) -> context.add(xmlTag), XmlTag.class, xmlTagList, false);
		}

		// XML-Tags verarbeiten
		Kontakt kontakt = new Kontakt();
		for (XmlTag xmlTag : xmlTagList) {
			String tagName = xmlTag.getName();

			if ("name".equals(tagName)) {
				kontakt.name = getStringContent(xmlTag);
			} else if ("geburtsname".equals(tagName)) {
				kontakt.geburtsname = getStringContent(xmlTag);
			} else if ("vorname".equals(tagName)) {
				addStringContent(xmlTag, kontakt.vornamen);
			} else if ("rufname".equals(tagName)) {
				addStringContent(xmlTag, kontakt.rufname);
			} else if ("titel".equals(tagName)) {
				kontakt.titel = getStringContent(xmlTag);
			} else if ("geburtstag".equals(tagName)) {
				kontakt.geburtstag = getStringContent(xmlTag);
			} else if ("todestag".equals(tagName)) {
				kontakt.todestag = getStringContent(xmlTag);
			} else if ("foto".equals(tagName)) {
				addStringContent(xmlTag, kontakt.fotos);
			} else if (getKommunikation(xmlTag, kontakt)) {
				// nichts weiter zu tun
			} else if ("adresse".equals(tagName)) {
				Adresse adresse = getAdresse(xmlTag);
				if (adresse != null) {
					kontakt.adressen.add(adresse);
				}
			} else if ("kategorie".equals(tagName)) {
				addStringContent(xmlTag, kontakt.kategorien);
			}
		}

		return kontakt;
	}

	/**
	 * Liest eine Adresse ein.
	 * 
	 * @return null -> Adresse ist leer oder abgelaufen.
	 */
	@SuppressWarnings("StatementWithEmptyBody")
	@Nullable
	private Adresse getAdresse(@NotNull XmlTag adresseTag) {
		// nur gültige Adressen anzeigen
		boolean gueltigBisGesetzt = adresseTag.getOptions().containsKey("gültigbis");
		Date gueltigBis = parseDate(adresseTag.getOptions().get("gültigbis"));
		if (gueltigBisGesetzt && (gueltigBis == null || gueltigBis.before(new Date()))) {
			return null;
		}

		// XML-Tags auf dieser Ebene suchen
		List<XmlTag> xmlTagList = new ArrayList<>();
		if (adresseTag.getChild() != null) {
			WikiHelper.traversePageElements(adresseTag.getChild(), (xmlTag, context) -> context.add(xmlTag), XmlTag.class, xmlTagList, false);
		}

		// XML-Tags verarbeiten
		Adresse adresse = new Adresse();
		for (XmlTag xmlTag : xmlTagList) {
			String tagName = xmlTag.getName();

			if ("name".equals(tagName)) {
				adresse.name = getStringContent(xmlTag);
			} else if ("straße".equals(tagName)) {
				adresse.strasse = getStringContent(xmlTag);
			} else if ("plz".equals(tagName)) {
				adresse.plz = getStringContent(xmlTag);
			} else if ("ort".equals(tagName)) {
				adresse.ort = getStringContent(xmlTag);
			} else if ("land".equals(tagName)) {
				adresse.land = getStringContent(xmlTag);
			} else if ("bundesland".equals(tagName)) {
				adresse.bundesland = getStringContent(xmlTag);
			} else if ("bezirk".equals(tagName)) {
				adresse.bezirk = getStringContent(xmlTag);
			} else if ("telefon".equals(tagName)) {
				addStringContent(xmlTag, adresse.telefon);
			} else if ("fax".equals(tagName)) {
				addStringContent(xmlTag, adresse.fax);
			} else if (getKommunikation(xmlTag, adresse)) {
				// nichts weiter zu tun
			} else if ("beschreibung".equals(tagName)) {
				adresse.beschreibung = xmlTag.getChild();
			} else if ("kategorie".equals(tagName)) {
				addStringContent(xmlTag, adresse.kategorien);
			}
		}

		return adresse;
	}

	/**
	 * Liest Felder zur Kommunikation ein. Diese können Bestandteil eines
	 * Kontaktes oder einer Adresse sein.
	 */
	private boolean getKommunikation(@NotNull XmlTag xmlTag, @NotNull Kommunikation kommunikation) {
		String tagName = xmlTag.getName();

		switch (tagName) {
			case "homepage":
				addStringContent(xmlTag, kommunikation.homepage);
				break;
			case "aim":
				addStringContent(xmlTag, kommunikation.aim);
				break;
			case "email":
				addStringContent(xmlTag, kommunikation.email);
				break;
			case "facebook":
				addStringContent(xmlTag, kommunikation.facebook);
				break;
			case "googletalk":
				addStringContent(xmlTag, kommunikation.googletalk);
				break;
			case "icq":
				addStringContent(xmlTag, kommunikation.icq);
				break;
			case "jabber":
				addStringContent(xmlTag, kommunikation.jabber);
				break;
			case "linkedin":
				addStringContent(xmlTag, kommunikation.linkedin);
				break;
			case "mobil":
				addStringContent(xmlTag, kommunikation.mobil);
				break;
			case "msn":
				addStringContent(xmlTag, kommunikation.msn);
				break;
			case "qq":
				addStringContent(xmlTag, kommunikation.qq);
				break;
			case "skype":
				addStringContent(xmlTag, kommunikation.skype);
				break;
			case "twitter":
				addStringContent(xmlTag, kommunikation.twitter);
				break;
			case "wechat":
				addStringContent(xmlTag, kommunikation.wechat);
				break;
			case "xing":
				addStringContent(xmlTag, kommunikation.xing);
				break;
			case "yahoo":
				addStringContent(xmlTag, kommunikation.yahoo);
				break;
			case "youtube":
				addStringContent(xmlTag, kommunikation.youtube);
				break;
			default:
				return false;
		}

		return true;
	}

	/**
	 * Gibt den Text-Inhalt einer Kontakteigenschaft als String zurück, wenn der
	 * Inhalt nicht als veraltet markiert ist.
	 * 
	 * @return Text-Inhalt des Tags. null -> kein Text oder Tag nicht mehr
	 *         gültig.
	 */
	@Nullable
	public static String getStringContent(@NotNull XmlTag xmlTag) {
		// nur gültige Tags anzeigen
		boolean gueltigBisGesetzt = xmlTag.getOptions().containsKey("gültigbis");
		Date gueltigBis = parseDate(xmlTag.getOptions().get("gültigbis"));
		if (gueltigBisGesetzt && (gueltigBis == null || gueltigBis.before(new Date()))) {
			return null;
		}

		String textValue = WikiHelper.getStringContent(xmlTag);
		if (textValue.length() > 0) {
			return textValue;
		} else {
			return null;
		}
	}

	/**
	 * Fügt den Text-Inhalt eines Tags in die Liste ein, sofern einer vorhanden
	 * ist.
	 */
	private void addStringContent(@NotNull XmlTag xmlTag, @NotNull List<String> list) {
		String content = getStringContent(xmlTag);
		if (content != null) {
			list.add(content);
		}
	}

	/**
	 * Erzeugt eine formatierte Ausgabe der Kontaktdaten.
	 */
	@NotNull
	private PageElementList formatKontakt(@NotNull Kontakt kontakt) {
		PageElementList result = new PageElementList();

		if (kontakt.fotos.size() > 0) {
			PageElementList imageContainer = new PageElementList();
			for (String foto : kontakt.fotos) {
				Map<String, String> options = new HashMap<>();
				options.put("class", "kontaktseite-bild");
				imageContainer.add(new Image(foto, options, null, null));
			}
			result.add(new HtmlTag("div", "class=\"kontaktseite-bilder\"", imageContainer));
		}
		if (kontakt.titel != null) {
			addString(result, kontakt.titel);
		}
		{
			PageElementList tagContent = new PageElementList();
			if (kontakt.name != null) {
				tagContent.add(new TextOnly(kontakt.name));
			}
			if (kontakt.geburtsname != null) {
				tagContent.add(new TextOnly(" (geb. " + kontakt.geburtsname + ")"));
			}
			if (!kontakt.vornamen.isEmpty()) {
				if (tagContent.size() > 0) {
					tagContent.add(new TextOnly(", "));
				}
				tagContent.add(new TextOnly(StringUtils.concat(kontakt.vornamen, " ")));
			}
			if (!kontakt.rufname.isEmpty()) {
				tagContent.add(new TextOnly(" (" + StringUtils.concat(kontakt.rufname, ", ") + ")"));
			}
			result.add(new Paragraph(false, 0, false, new Bold(tagContent, null, null), null, null));
		}
		if (kontakt.geburtstag != null || kontakt.todestag != null) {
			PageElementList tagContent = new PageElementList();
			if (kontakt.geburtstag != null) {
				tagContent.add(new TextOnly("* " + kontakt.geburtstag));
			}
			if (kontakt.geburtstag != null && kontakt.todestag != null) {
				tagContent.add(new Html(" &nbsp; "));
			}
			if (kontakt.todestag != null) {
				tagContent.add(new Html("&dagger; "));
				tagContent.add(new TextOnly(kontakt.todestag));
			}
			formatAlter(tagContent, kontakt.geburtstag, kontakt.todestag);
			result.add(new Paragraph(false, 0, false, tagContent, null, null));
		}

		PageElementList sectionContent1 = new PageElementList();
		formatKommunikation(sectionContent1, kontakt);
		if (sectionContent1.size() > 0) {
			result.add(new VerticalSpace());
			result.add(sectionContent1);
		}

		for (Adresse adresse : kontakt.adressen) {
			PageElementList sectionContent2 = formatAdresse(adresse);
			if (sectionContent2 != null) {
				result.add(sectionContent2);
			}
		}

		if (!kontakt.kategorien.isEmpty()) {
			result.add(new VerticalSpace());
			addStringList(result, kontakt.kategorien, "Kontakt-Kategorien", null);
		}

		return result;
	}

	private void formatAlter(@NotNull PageElementList outputList, @Nullable String geburtstag, @Nullable String todestag) {
		if (geburtstag == null) {
			return;
		}

		// Datumsangaben parsen
		Date geburtstagDate;
		Date todestagDate;
		try {
			synchronized (DATE_FORMAT) {
				geburtstagDate = DATE_FORMAT.parse(geburtstag);
			}
			if (todestag != null) {
				synchronized (DATE_FORMAT) {
					todestagDate = DATE_FORMAT.parse(todestag);
				}
			} else {
				todestagDate = new Date(); // Alter bis heute berechnen
			}
		} catch (ParseException e) {
			return; // bei Fehler keine Berechnung des Alters
		}

		// Jahr und Tag extrahieren
		Calendar cal = Calendar.getInstance(Locale.GERMANY);
		cal.setTime(geburtstagDate);
		int fromYear = cal.get(Calendar.YEAR);
		int fromDayOfYear = cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(todestagDate);
		int toYear = cal.get(Calendar.YEAR);
		int toDayOfYear = cal.get(Calendar.DAY_OF_YEAR);

		// Alter in Jahren berechnen
		int ageYears = toYear - fromYear;
		if (toDayOfYear < fromDayOfYear) {
			ageYears--;
		}

		// Tage seit dem letzten Geburtstag berechnen
		int ageDaysOfYear = toDayOfYear - fromDayOfYear;
		if (ageDaysOfYear < 0) {
			// Schaltjahre sind hier unwichtig, weil nur
			// eine ungefähre Ausgabe erfolgt
			ageDaysOfYear += 365;
		}

		// Alter mit Nachkommastellen berechnen
		float age = (float) ageYears + ((float) ageDaysOfYear) / 365;

		// Alter ausgeben
		NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);
		nf.setMinimumFractionDigits(1);
		nf.setMaximumFractionDigits(1);
		String ageStr = nf.format(age);
		outputList.add(new TextOnly(" (" + ageStr + ")"));
	}

	@Nullable
	private PageElementList formatAdresse(@NotNull Adresse adresse) {
		PageElementList cell1Content = new PageElementList();
		if (adresse.name != null) {
			addString(cell1Content, adresse.name);
		}

		if (adresse.strasse != null) {
			addString(cell1Content, adresse.strasse);
		}

		if (adresse.plz != null || adresse.ort != null) {
			PageElementList tagContent = new PageElementList();
			if (adresse.plz != null) {
				tagContent.add(new TextOnly(adresse.plz));
			}
			if (adresse.plz != null && adresse.ort != null) {
				tagContent.add(new TextOnly(" "));
			}
			if (adresse.ort != null) {
				tagContent.add(new TextOnly(adresse.ort));
			}
			cell1Content.add(new Paragraph(false, 0, false, tagContent, null, null));
		}

		if (adresse.land != null || adresse.bundesland != null || adresse.bezirk != null) {
			PageElementList tagContent = new PageElementList();
			if (adresse.land != null) {
				tagContent.add(new TextOnly(adresse.land));
			}
			if (adresse.bundesland != null || adresse.bezirk != null) {
				if (adresse.land != null) {
					tagContent.add(new TextOnly(" "));
				}
				tagContent.add(new TextOnly("("));
				if (adresse.bundesland != null) {
					tagContent.add(new TextOnly(adresse.bundesland));
				}
				if (adresse.bundesland != null && adresse.bezirk != null) {
					tagContent.add(new TextOnly(", "));
				}
				if (adresse.bezirk != null) {
					tagContent.add(new TextOnly(adresse.bezirk));
				}
				tagContent.add(new TextOnly(")"));
			}
			cell1Content.add(new Paragraph(false, 0, false, tagContent, null, null));
		}

		if (adresse.beschreibung != null) {
			PageElementList tagContent = new PageElementList();
			tagContent.add(new Italic(adresse.beschreibung, null, null));
			cell1Content.add(new Paragraph(false, 0, false, tagContent, null, null));
		}

		PageElementList cell2Content = new PageElementList();
		if (!adresse.telefon.isEmpty()) {
			addStringList(cell2Content, adresse.telefon, "Telefon", null);
		}
		if (!adresse.fax.isEmpty()) {
			addStringList(cell2Content, adresse.fax, "Fax", null);
		}
		formatKommunikation(cell2Content, adresse);
		if (!adresse.kategorien.isEmpty()) {
			addStringList(cell2Content, adresse.kategorien, "Kategorien", null);
		}

		if (cell1Content.size() > 0 || cell2Content.size() > 0) {
			Table table = new Table("kontaktseite-adresse", null, null);
			table.newRow(null);
			table.addCell(new TableCell(cell1Content, false, null));
			table.addCell(new TableCell(cell2Content, false, null));

			PageElementList result = new PageElementList();
			result.add(new VerticalSpace());
			result.add(table);
			return result;
		} else {
			return null;
		}
	}

	private void formatKommunikation(@NotNull PageElementList outputList, @NotNull Kommunikation kommunikation) {
		if (!kommunikation.homepage.isEmpty()) {
			addStringList(outputList, kommunikation.homepage, "Homepage", "http://");
		}
		if (!kommunikation.mobil.isEmpty()) {
			addStringList(outputList, kommunikation.mobil, "Mobil", null);
		}
		if (!kommunikation.email.isEmpty()) {
			addStringList(outputList, kommunikation.email, "E-Mail", "mailto:");
		}
		if (!kommunikation.aim.isEmpty()) {
			addStringList(outputList, kommunikation.aim, "AIM", null);
		}
		if (!kommunikation.facebook.isEmpty()) {
			addStringListFacebook(outputList, kommunikation.facebook);
		}
		if (!kommunikation.googletalk.isEmpty()) {
			addStringList(outputList, kommunikation.googletalk, "Google Talk", null);
		}
		if (!kommunikation.icq.isEmpty()) {
			addStringList(outputList, kommunikation.icq, "ICQ", "http://www.icq.com/people/");
		}
		if (!kommunikation.jabber.isEmpty()) {
			addStringList(outputList, kommunikation.jabber, "Jabber", null);
		}
		if (!kommunikation.linkedin.isEmpty()) {
			addStringListLinkedIn(outputList, kommunikation.linkedin);
		}
		if (!kommunikation.msn.isEmpty()) {
			addStringList(outputList, kommunikation.msn, "MSN", null);
		}
		if (!kommunikation.qq.isEmpty()) {
			addStringList(outputList, kommunikation.qq, "QQ", null);
		}
		if (!kommunikation.skype.isEmpty()) {
			addStringList(outputList, kommunikation.skype, "Skype", "skype:");
		}
		if (!kommunikation.twitter.isEmpty()) {
			addStringList(outputList, kommunikation.twitter, "Twitter", "http://twitter.com/");
		}
		if (!kommunikation.wechat.isEmpty()) {
			addStringList(outputList, kommunikation.wechat, "WeChat", null);
		}
		if (!kommunikation.xing.isEmpty()) {
			addStringList(outputList, kommunikation.xing, "Xing", "https://www.xing.com/profile/");
		}
		if (!kommunikation.yahoo.isEmpty()) {
			addStringList(outputList, kommunikation.yahoo, "Yahoo", null);
		}
		if (!kommunikation.youtube.isEmpty()) {
			addStringList(outputList, kommunikation.youtube, "Youtube", "http://www.youtube.com/user/");
		}
	}

	/**
	 * Erzeugt eine Ausgabe aus einem String.
	 * 
	 * @param outputList Elementliste für die Ausgabe.
	 * @param string String, der ausgegeben werden soll.
	 */
	private void addString(@NotNull PageElementList outputList, @NotNull String string) {
		PageElementList tagContent = new PageElementList();
		tagContent.add(new TextOnly(string));
		outputList.add(new Paragraph(false, 0, false, tagContent, null, null));
	}

	/**
	 * Erzeugt eine Ausgabe aus einer String-Liste.
	 * 
	 * @param outputList Elementliste für die Ausgabe.
	 * @param stringList String-Liste, die ausgegeben werden soll.
	 * @param sectionName Name, der vor die Zeile geschrieben werden soll.
	 * @param linkPrefix Wenn nicht null, werden die Strings extern verlinkt.
	 */
	private void addStringList(@NotNull PageElementList outputList, @NotNull List<String> stringList, @Nullable String sectionName, @Nullable String linkPrefix) {
		PageElementList tagContent = new PageElementList();

		if (sectionName != null) {
			tagContent.add(new Bold(new TextOnly(sectionName + ": "), null, null));
		}

		for (int i = 0; i < stringList.size(); i++) {
			String str = stringList.get(i);
			if (i >= 1) {
				tagContent.add(new TextOnly(", "));
			}
			if (linkPrefix != null) {
				// als Link darstellen
				String url = str;
				if (!url.startsWith(linkPrefix)) {
					url = linkPrefix + url;
				}
				tagContent.add(new LinkExternal(url, new TextOnly(str), null, null));
			} else {
				tagContent.add(new TextOnly(str));
			}
		}

		outputList.add(new Paragraph(false, 0, false, tagContent, null, null));
	}

	/**
	 * Erzeugt eine Ausgabe aus einer String-Liste speziell für Facebook-Links.
	 * <ul>
	 * <li>Kurzform: <tt>http://www.facebook.com/zuck</tt></li>
	 * <li>Langform:
	 * <tt>http://www.facebook.com/people/Mark-Zuckerberg/123456789</tt></li>
	 * <li>Länderspezifischer Link: <tt>http://ja-jp.facebook.com/kleinchi</tt></li>
	 * </ul>
	 * 
	 * @param outputList Elementliste für die Ausgabe.
	 * @param stringList String-Liste, die ausgegeben werden soll.
	 */
	private void addStringListFacebook(@NotNull PageElementList outputList, @NotNull List<String> stringList) {
		PageElementList tagContent = new PageElementList();

		tagContent.add(new Bold(new TextOnly("Facebook: "), null, null));

		for (int i = 0; i < stringList.size(); i++) {
			String str = stringList.get(i);
			if (i >= 1) {
				tagContent.add(new TextOnly(", "));
			}
			// als Link darstellen
			String url;
			if (str.startsWith("http://")) {
				url = str; // URL ist bereits komplett
			} else if (str.indexOf('/') >= 0) {
				url = "http://www.facebook.com/people/" + str;
			} else {
				url = "http://www.facebook.com/" + str;
			}
			tagContent.add(new LinkExternal(url, new TextOnly(str), null, null));
		}

		outputList.add(new Paragraph(false, 0, false, tagContent, null, null));
	}

	/**
	 * Erzeugt eine Ausgabe aus einer String-Liste speziell für LinkedIn-Links.
	 * <ul>
	 * <li>Öffentliche Seite: <tt>http://www.linkedin.com/in/jeffweiner08</tt></li>
	 * <li>Vollständiges Profil:
	 * <tt>http://www.linkedin.com/profile/view?id=22330283</tt></li>
	 * </ul>
	 * 
	 * @param outputList Elementliste für die Ausgabe.
	 * @param stringList String-Liste, die ausgegeben werden soll.
	 */
	private void addStringListLinkedIn(@NotNull PageElementList outputList, @NotNull List<String> stringList) {
		PageElementList tagContent = new PageElementList();

		tagContent.add(new Bold(new TextOnly("LinkedIn: "), null, null));

		for (int i = 0; i < stringList.size(); i++) {
			String str = stringList.get(i);
			if (i >= 1) {
				tagContent.add(new TextOnly(", "));
			}
			// als Link darstellen
			String url;
			if (StringUtils.isNumeric(str)) {
				url = "http://www.linkedin.com/profile/view?id=" + str;
			} else {
				url = "http://www.linkedin.com/in/" + str;
			}
			tagContent.add(new LinkExternal(url, new TextOnly(str), null, null));
		}

		outputList.add(new Paragraph(false, 0, false, tagContent, null, null));
	}

	/**
	 * Parst eine Datumsangabe. Wenn sie kein gültiges Format hat, wird
	 * <code>null</code> zurückgegeben.
	 * 
	 * @param dateString Datumsangabe.
	 * @return Datum. <code>null</code> --> Datumsangabe war ungültig.
	 */
	@Nullable
	private static Date parseDate(@Nullable String dateString) {
		if (dateString == null) {
			return null;
		}

		try {
			synchronized (DATE_FORMAT) {
				return DATE_FORMAT.parse(dateString);
			}
		} catch (ParseException e) {
			// Datum hat kein gültiges Format
			return null;
		}
	}

	/**
	 * Enthält die Kontaktdaten zu einem Kontakt oder einer Adresse.
	 */
	private abstract static class Kommunikation {
		public final List<String> aim = new ArrayList<>();
		public final List<String> email = new ArrayList<>();
		public final List<String> facebook = new ArrayList<>();
		public final List<String> googletalk = new ArrayList<>();
		public final List<String> homepage = new ArrayList<>();
		public final List<String> icq = new ArrayList<>();
		public final List<String> jabber = new ArrayList<>();
		public final List<String> linkedin = new ArrayList<>();
		public final List<String> mobil = new ArrayList<>();
		public final List<String> msn = new ArrayList<>();
		public final List<String> qq = new ArrayList<>();
		public final List<String> skype = new ArrayList<>();
		public final List<String> twitter = new ArrayList<>();
		public final List<String> wechat = new ArrayList<>();
		public final List<String> xing = new ArrayList<>();
		public final List<String> yahoo = new ArrayList<>();
		public final List<String> youtube = new ArrayList<>();
	}

	/**
	 * Enthält die kompletten Daten zu einem Kontakt.
	 */
	private static class Kontakt extends Kommunikation {
		public String name;
		public String geburtsname;
		public final List<String> vornamen = new ArrayList<>();
		public final List<String> rufname = new ArrayList<>();
		public String titel;
		public String geburtstag;
		public String todestag;
		public final List<String> fotos = new ArrayList<>();

		public final List<Adresse> adressen = new ArrayList<>();
		public final List<String> kategorien = new ArrayList<>();
	}

	/**
	 * Enthält eine einzelne Anschrift zu einem Kontakt.
	 */
	private static class Adresse extends Kommunikation {
		public String name;
		public String strasse;
		public String plz;
		public String ort;
		public String land;
		public String bundesland;
		public String bezirk;

		public final List<String> telefon = new ArrayList<>();
		public final List<String> fax = new ArrayList<>();
		public PageElement beschreibung;
		public final List<String> kategorien = new ArrayList<>();
	}
}
