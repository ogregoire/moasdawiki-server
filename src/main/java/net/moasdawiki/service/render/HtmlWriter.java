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

package net.moasdawiki.service.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.moasdawiki.util.EscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class to generate valid HTML.
 */
public class HtmlWriter {

	/**
	 * HTTP request methods (we only need those two)
	 */
	public enum Method {
		GET, POST
	}

	/**
	 * Web page title; null -> no title.
	 */
	@Nullable
	private String title;

	/**
	 * HTML body tag attributes; null -> no attributes.
	 */
	@Nullable
	private String bodyParams;

	/**
	 * HTML body content.
	 */
	@NotNull
	private final ArrayList<String> bodyText;

	/**
	 * Open HTML tag stack.
	 */
	@NotNull
	private final Stack<String> tagStack;

	/**
	 * Add line break before adding text?
	 */
	private boolean continueInNewLine;

	/**
	 * Constructor.
	 */
	public HtmlWriter() {
		super();
		bodyText = new ArrayList<>();
		tagStack = new Stack<>();
		// add line break at the beginning
		continueInNewLine = true;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getBodyParams() {
		return bodyParams;
	}

	public void setBodyParams(@Nullable String bodyParams) {
		this.bodyParams = bodyParams;
	}

	public void setContinueInNewLine() {
		continueInNewLine = true;
	}

	/**
	 * Add raw HTML content.
	 */
	public void htmlText(@Nullable String text) {
		if (continueInNewLine) {
			StringBuilder s = new StringBuilder();
			for (int i = 1; i <= tagStack.size(); i++) {
				s.append("  ");
			}
			bodyText.add(s.toString());

			continueInNewLine = false;
		}

		if (text != null) {
			int index = bodyText.size() - 1;
			String line = bodyText.get(index);
			line += text;
			bodyText.set(index, line);
		}
	}

	/**
	 * Add a HTML line break &lt;br&gt;.
	 */
	public void htmlNewLine() {
		htmlText("<br>");
		setContinueInNewLine();
	}

	/**
	 * Open a HTML tag.
	 *
	 * @param tagName HTML tag name.
	 * @return Tag stack depth before the new tag.
	 *         Can be used with {@link #closeTags(int)} to close several tags at once.
	 */
	public int openTag(@NotNull String tagName) {
		return openTag(tagName, null);
	}

	/**
	 * Open a HTML tag.
	 *
	 * @param tagName HTML tag name.
	 * @param params HTML tag attributes. null -> no attributes.
	 * @return Tag stack depth before the new tag.
	 *         Can be used with {@link #closeTags(int)} to close several tags at once.
	 */
	public int openTag(@NotNull String tagName, @Nullable String params) {
		if (params != null && params.length() > 0) {
			htmlText("<" + tagName + " " + params + ">");
		} else {
			htmlText("<" + tagName + ">");
		}
		tagStack.push(tagName);
		return tagStack.size() - 1;
	}

	/**
	 * Open a HTML div tag.
	 */
	public int openDivTag(@NotNull String stylesheetClass) {
		return openDivTag(stylesheetClass, null);
	}

	/**
	 * Open a HTML div tag.
	 */
	public int openDivTag(@Nullable String stylesheetClass, @Nullable String params) {
		StringBuilder tagParams = new StringBuilder();
		if (stylesheetClass != null) {
			tagParams.append("class=\"");
			tagParams.append(EscapeUtils.escapeHtml(stylesheetClass));
			tagParams.append("\"");
		}
		if (params != null) {
			if (tagParams.length() > 0) {
				tagParams.append(' ');
			}
			tagParams.append(params);
		}
		return openTag("div", tagParams.toString());
	}

	/**
	 * Open a HTML span tag.
	 */
	public int openSpanTag(@Nullable String stylesheetClass) {
		return openTag("span", "class=\"" + EscapeUtils.escapeHtml(stylesheetClass) + "\"");
	}

	/**
	 * Open a HTML form tag.
	 */
	@SuppressWarnings("UnusedReturnValue")
	public int openFormTag(@Nullable String name) {
		return openFormTag(name, null, null);
	}

	/**
	 * Open a HTML form tag.
	 */
	public int openFormTag(@Nullable String name, @Nullable String action, @Nullable Method method) {
		StringBuilder params = new StringBuilder();
		params.append("method=\"");
		if (method == Method.GET) {
			params.append(EscapeUtils.escapeHtml("get"));
		} else {
			params.append(EscapeUtils.escapeHtml("post"));
		}
		params.append('"');
		if (action != null) {
			params.append(" action=\"");
			params.append(EscapeUtils.escapeHtml(action));
			params.append('"');
		}
		params.append(" enctype=\"application/x-www-form-urlencoded\"");
		if (name != null) {
			params.append(" name=\"");
			params.append(EscapeUtils.escapeHtml(name));
			params.append('"');
		}
		return openTag("form", params.toString());
	}

	/**
	 * Close the last opened HTML tag.
	 * If no HTML tag is open nothing happens.
	 */
	public void closeTag() {
		if (!tagStack.empty()) {
			String tag = tagStack.pop();
			htmlText("</" + tag + ">");
		}
	}

	/**
	 * Close all open HTML tags until the given stack depth.
	 *
	 * @param stackDepth Stack depth; 0 -> close all open tags.
	 */
	public void closeTags(int stackDepth) {
		while (tagStack.size() > stackDepth) {
			closeTag();
		}
	}

	/**
	 * Close all open HTML tags.
	 */
	public void closeAllTags() {
		closeTags(0);
	}

	/**
	 * Return the last opened HTML tag that hasn't been closed.
	 * Returns null if there is no open tag.
	 */
	@Nullable
	public String getCurrentTag() {
		return getCurrentTag(0);
	}

	/**
	 * Return the last opened HTML tag at the given relative stack depth.
	 * Returns null if there is no open tag.
	 *
	 * @param downStack Number of stack levels below current level; 0 -> current level.
	 */
	@Nullable
	public String getCurrentTag(int downStack) {
		if (tagStack.size() > downStack && downStack >= 0) {
			return tagStack.elementAt(tagStack.size() - 1 - downStack);
		} else {
			// not in stack
			return null;
		}
	}

	/**
	 * Add HTML content of a {@link HtmlWriter}.
	 */
	public void addHtmlWriter(@NotNull HtmlWriter htmlWriter) {
		htmlWriter.closeAllTags();
		for (String line : htmlWriter.bodyText) {
			setContinueInNewLine();
			htmlText(line);
		}
		setContinueInNewLine();
	}

	@NotNull
	public List<String> getBodyLines() {
		return bodyText;
	}
}
