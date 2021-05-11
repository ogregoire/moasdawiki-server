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

package net.moasdawiki.service.search;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.service.repository.RepositoryService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.util.PathUtils;
import net.moasdawiki.util.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Implements a full-text search for the wiki repository.
 */
public class SearchService {

	@NotNull
	private final Logger logger;

	@NotNull
	private final WikiService wikiService;

	@NotNull
	private final SearchIgnoreList searchIgnoreList;

	@NotNull
	private final SearchIndex searchIndex;

	/**
	 * Is repository scanning allowed to update the cache content?
	 * Is set to false for the App as the cache file is updated by synchronization.
	 */
	private final boolean repositoryScanAllowed;

	/**
	 * Constructor.
	 */
	public SearchService(@NotNull Logger logger, @NotNull RepositoryService repositoryService,
						 @NotNull WikiService wikiService, boolean repositoryScanAllowed) {
		super();
		this.logger = logger;
		this.wikiService = wikiService;
		this.searchIgnoreList = new SearchIgnoreList(logger, repositoryService);
		this.searchIndex = new SearchIndex(logger, repositoryService, wikiService, searchIgnoreList, repositoryScanAllowed);
		this.repositoryScanAllowed = repositoryScanAllowed;
	}

	/**
	 * Drops the cache content.
	 * Is called in App environment after synchronization with server.
	 */
	public void reset() {
		searchIgnoreList.reset();
		searchIndex.reset();
	}

	/**
	 * Parses a query string. Supported use cases:
	 *
	 * <ul>
	 * <li><code>Word</code>: Search for a single word</li>
	 * <li><code>Word1 word2</code>: Search for several words, all of them must match</li>
	 * </ul>
	 * 
	 * @param query Query string.
	 * @return Word list.
	 */
	@Contract(pure = true)
	@NotNull
	public static Set<String> parseQueryString(@NotNull String query) {
		Set<String> result = new HashSet<>();

		StringBuilder sb = new StringBuilder();
		IntStream codePointStream = query.codePoints();
		PrimitiveIterator.OfInt it = codePointStream.iterator();
		while (it.hasNext()) {
			int codePoint = it.nextInt();
			if (Character.isLetterOrDigit(codePoint)) {
				// inside word
				sb.appendCodePoint(codePoint);
			} else {
				// separator found -> end of word
				if (sb.length() > 0) {
					result.add(sb.toString());
					sb.setLength(0);
				}
			}
		}

		// add last word
		if (sb.length() > 0) {
			result.add(sb.toString());
		}

		return result;
	}

	/**
	 * Find all wiki pages that match the search query.
	 * The string match considers similar looking characters as identical, see {@link #generateNormalizedPattern(Set)}.
	 */
	@NotNull
	public List<PageDetails> searchInRepository(@NotNull Set<String> words) throws ServiceException {
		if (words.isEmpty()) {
			// No words specified --> return empty result
			return Collections.emptyList();
		}
		Set<String> wikiFilePaths = searchIndex.searchWikiFilePaths(words);
		return scanWikiPages(wikiFilePaths, words);
	}

	/**
	 * Highlight the search words in the given wiki pages.
	 */
	@NotNull
	List<PageDetails> scanWikiPages(@NotNull Set<String> wikiFilePaths, @NotNull Set<String> words) throws ServiceException {
		// Normalize search words and combine to a single pattern
		Pattern searchPattern = generateNormalizedPattern(words);

		// scan wiki pages
		List<PageDetails> pageDetailsList = new ArrayList<>();
		for (String wikiFilePath : wikiFilePaths) {
			String wikiText;
			if (repositoryScanAllowed) {
				WikiFile wikiFile = wikiService.getWikiFile(wikiFilePath);
				wikiText = wikiFile.getWikiText();
			} else {
				wikiText = ""; // don't highlight content matches
			}
			PageDetails pageDetails = scanPage(wikiFilePath, wikiText, searchPattern);
			pageDetailsList.add(pageDetails);
		}

		// Sort matches by descending relevance
		pageDetailsList.sort((p1, p2) -> {
			int cmp = Integer.compare(p1.getRelevance(), p2.getRelevance());
			if (cmp != 0) {
				return -cmp; // descending
			} else {
				return String.CASE_INSENSITIVE_ORDER.compare(p1.getPagePath(), p2.getPagePath());
			}
		});
		logger.write("Search result contains " + pageDetailsList.size() + " wiki pages");
		return pageDetailsList;
	}

	/**
	 * Collects all matching positions in a wiki page.
	 */
	@NotNull
	private PageDetails scanPage(@NotNull String pagePath, @NotNull String wikiText, @NotNull Pattern searchPattern) throws ServiceException {
		// Collect matches in page name
		MatchingCategories mc = new MatchingCategories();
		PageDetails.MatchingLine titleLine = scanPageTitle(pagePath, searchPattern, mc);

		// Collect matches in page content
		List<PageDetails.MatchingLine> textLines = scanPageText(pagePath, wikiText, searchPattern, mc);

		// Calculate relevance
		int relevance = calculateRelevance(mc);
		return new PageDetails(pagePath, titleLine, textLines, relevance);
	}

	@NotNull
	private PageDetails.MatchingLine scanPageTitle(@NotNull String pagePath, @NotNull Pattern searchPattern, @NotNull MatchingCategories mc) {
		PageDetails.MatchingLine result = new PageDetails.MatchingLine(pagePath);

		// Normalize wiki page
		String pagePathNorm = StringUtils.unicodeNormalize(pagePath);
		String pageName = PathUtils.extractWebName(pagePath);
		String pageNameNorm = StringUtils.unicodeNormalize(pageName);

		Matcher mPath = searchPattern.matcher(pagePathNorm);
		Matcher mName = searchPattern.matcher(pageNameNorm);
		if (mPath.matches()) {
			// Page name including path matches
			mc.titleComplete = true;
			mc.titleWord++;
			if (mPath.start() < mPath.end()) {
				result.getPositions().add(new PageDetails.Marker(mPath.start(), mPath.end()));
			}

		} else if (mName.matches()) {
			// Page name (after last '/') matches
			mc.titleComplete = true;
			mc.titleWord++;
			int prefixLen = pagePath.length() - pageName.length();
			if (mName.start() < mName.end()) {
				result.getPositions().add(new PageDetails.Marker(prefixLen + mName.start(), prefixLen + mName.end()));
			}

		} else {
			// Substring match
			mPath.reset();
			while (mPath.find()) {
				if (isWordAligned(pagePathNorm, mPath)) {
					mc.titleWord++;
				} else {
					mc.titleSubstring++;
				}
				if (mPath.start() < mPath.end()) {
					result.getPositions().add(new PageDetails.Marker(mPath.start(), mPath.end()));
				}
			}
		}
		return result;
	}

	@NotNull
	private List<PageDetails.MatchingLine> scanPageText(@NotNull String pagePath, @NotNull String wikiText,
														@NotNull Pattern searchPattern, @NotNull MatchingCategories mc) throws ServiceException {
		try {
			List<PageDetails.MatchingLine> textLines = new ArrayList<>();
			BufferedReader reader = new BufferedReader(new StringReader(wikiText));
			String line = reader.readLine();
			while (line != null) {
				PageDetails.MatchingLine matchingLine = null;
				String lineNorm = StringUtils.unicodeNormalize(line);
				Matcher m = searchPattern.matcher(lineNorm);
				while (m.find()) {
					if (m.start() < m.end()) {
						if (isHeading(lineNorm)) {
							// Match in heading
							if (isWordAligned(lineNorm, m)) {
								mc.headingWord++;
							} else {
								mc.headingSubstring++;
							}
						} else {
							// Match in normal paragraph
							if (isWordAligned(lineNorm, m)) {
								mc.paragraphWord++;
							} else {
								mc.paragraphSubstring++;
							}
						}

						if (matchingLine == null) {
							matchingLine = new PageDetails.MatchingLine(line);
							textLines.add(matchingLine);
						}
						if (m.start() < m.end()) {
							matchingLine.getPositions().add(new PageDetails.Marker(m.start(), m.end()));
						}
					}
				}
				line = reader.readLine();
			}
			return textLines;
		} catch (IOException e) {
			// shouldn't occur
			String message = "Error scanning wiki page '" + pagePath + "'";
			logger.write(message, e);
			throw new ServiceException(message, e);
		}
	}

	/**
	 * Combines the search strings to a regular expression.
	 * Similar looking characters are considered to be identical,
	 * diacritical characters and upper/lower case are ignored.
	 * Example: ä=ae, ö=oe, ü=ue, ß=ss, a=à=á=â=A=À=Á=Â, etc.
	 */
	@NotNull
	private Pattern generateNormalizedPattern(@NotNull Set<String> words) throws ServiceException {
		StringBuilder combined = new StringBuilder();
		for (String findStr : words) {
			// Umlaute and ß/ss to be similar
			findStr = expandUmlaute(findStr);

			// Normalize unicode characters
			findStr = StringUtils.unicodeNormalize(findStr);

			// Ignore upper/lower case, with unicode handling
			findStr = "?iu:" + findStr;

			// combine expressions
			if (combined.length() > 0) {
				combined.append('|');
			}
			combined.append('(').append(findStr).append(')');
		}

		try {
			return Pattern.compile(combined.toString());
		} catch (Exception e) {
			throw new ServiceException("Syntax error in regular search expression: " + combined, e);
		}
	}

	/**
	 * Adds alternative spellings for Umlaute and special characters
	 * to make them considered as identical (without diacritical characters).
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
	 * Checks if the match is aligned at word boundaries.
	 * Otherwise, it is a substring match.
	 *
	 * @param text Matching text.
	 * @param matcher Corresponding Matcher.
	 * @return <code>true</code> if match is aligned at word boundaries.
	 */
	private boolean isWordAligned(@NotNull String text, @NotNull Matcher matcher) {
		int start = matcher.start();
		int end = matcher.end();
		return (start == 0 || !Character.isLetter(text.codePointAt(start - 1))) && (end == text.length() || !Character.isLetter(text.codePointAt(end)));
	}

	/**
	 * Checks if the paragraph is a heading.
	 * I.e. starts with "= ".
	 * 
	 * @param text Paragraph.
	 * @return <code>true</code> if paragraph is a heading.
	 */
	private boolean isHeading(@NotNull String text) {
		return text.startsWith("=");
	}

	/**
	 * Calculates the relevance from the number of occurrences by category.
	 * Rules to be applied:
	 * 
	 * <ul>
	 * <li><code>titleComplete</code> counts more than <code>titleWord</code></li>
	 * <li><code>titleWord</code> counts more than <code>headingWord</code></li>
	 * <li><code>headingWord</code> counts more than <code>paragraphWord</code></li>
	 * <li><code>paragraphWord</code> counts more than <code>titleSubstring</code>
	 * </li>
	 * <li><code>titleSubstring</code> counts more than <code>headingSubstring</code></li>
	 * <li><code>headingSubstring</code> counts more than <code>paragraphSubstring</code></li>
	 * <li>within a single category the match count is used</li>
	 * <li>a match count > 9 is considered as 9</li>
	 * </ul>
	 * 
	 * @param mc Match count by category.
	 * @return Relevance >= <code>0</code>. <code>0</code> = no match.
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
	 * Match counts by category.
	 * <br>
	 * <code>...Complete</code> = Matches whole paragraph<br>
	 * <code>...Word</code> = Matches word boundaries<br>
	 * <code>...Substring</code> = Matches substring
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
