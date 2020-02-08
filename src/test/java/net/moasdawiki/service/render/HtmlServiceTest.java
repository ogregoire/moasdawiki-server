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

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.plugin.PluginService;
import net.moasdawiki.server.HttpResponse;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class HtmlServiceTest {

    private Settings settings;
    private Messages messages;
    private WikiService wikiService;
    private PluginService pluginService;

    private HtmlService htmlService;

    @BeforeMethod
    public void setUp() {
        settings = mock(Settings.class);
        when(settings.getProgramName()).thenReturn("ProgName");
        messages = mock(Messages.class);
        when(messages.getMessage(any(), any())).thenAnswer(invocation -> {
            String result = invocation.getArgument(0);
            if (invocation.getArguments().length >= 2) {
                result += "_" + invocation.getArgument(1);
            }
            return result;
        });
        wikiService = mock(WikiService.class);
        pluginService = mock(PluginService.class);
        when(pluginService.applyTransformations(any())).thenAnswer(invocation -> invocation.getArgument(0));
        htmlService = new HtmlService(new Logger(null), settings, messages, wikiService, pluginService);
    }

    @Test
    public void testConvertHtmlMinimal() {
        HttpResponse httpResponse = htmlService.convertHtml(new HtmlWriter());
        String response = new String(httpResponse.getContent(), StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>ProgName</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "</body>\n" +
                "</html>\n";
        assertEquals(response, expected);
    }

    @Test
    public void testConvertHtmlImportHeader() throws Exception {
        when(settings.getHtmlHeaderPagePath()).thenReturn("/HtmlHeaderPath");
        String wikiText = "html header line 1\n"
                + "html header line 2";
        when(wikiService.getWikiFile("/HtmlHeaderPath")).thenReturn(new WikiFile("/HtmlHeaderPath", wikiText, new WikiPage(null, null, null, null), new AnyFile("/HtmlHeaderPath")));
        HttpResponse httpResponse = htmlService.convertHtml(new HtmlWriter());
        String response = new String(httpResponse.getContent(), StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>ProgName</title>\n" +
                "html header line 1\n" +
                "html header line 2</head>\n" +
                "<body>\n" +
                "</body>\n" +
                "</html>\n";
        assertEquals(response, expected);
    }

    @Test
    public void testConvertHtmlBody() {
        HtmlWriter htmlWriter = new HtmlWriter();
        htmlWriter.setBodyParams("param1=value1 param2=value2");
        htmlWriter.htmlText("body text");
        HttpResponse httpResponse = htmlService.convertHtml(htmlWriter);
        String response = new String(httpResponse.getContent(), StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>ProgName</title>\n" +
                "</head>\n" +
                "<body param1=value1 param2=value2>\n" +
                "  body text\n" +
                "</body>\n" +
                "</html>\n";
        assertEquals(response, expected);
    }

    @Test
    public void testConvertPage() {
        WikiPage wikiPage = new WikiPage("/pagePath", new TextOnly("text only"), null, null);
        HttpResponse httpResponse = htmlService.convertPage(wikiPage);
        String response = new String(httpResponse.getContent(), StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>pagePath | ProgName</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  text only\n" +
                "</body>\n" +
                "</html>\n";
        assertEquals(response, expected);
        verify(pluginService, times(1)).applyTransformations(any());
    }

    @Test
    public void testGenerateRedirectToWikiPage() throws Exception {
        HttpResponse httpResponse = htmlService.generateRedirectToWikiPage("/pagePath");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpResponse.writeResponse(out);
        String response = out.toString("UTF-8");
        assertTrue(response.contains("HTTP/1.1 302 Moved Temporarily"));
        assertTrue(response.contains("Location: /view/pagePath"));
    }

    @Test
    public void testGenerateMessagePage() throws Exception {
        HttpResponse httpResponse = htmlService.generateMessagePage("key", "arg");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpResponse.writeResponse(out);
        String response = out.toString("UTF-8");
        String expectedBody = "<body>\n" +
                "  <b>key_arg</b>\n" +
                "</body>";
        assertTrue(response.contains(expectedBody));
        verify(messages, times(1)).getMessage(eq("key"), eq("arg"));
    }

    @Test
    public void testGenerateErrorPage() throws Exception {
        HttpResponse httpResponse = htmlService.generateErrorPage(404, "key", "arg");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpResponse.writeResponse(out);
        String response = out.toString("UTF-8");
        String expectedBody = "<body>\n" +
                "  <b>wiki.errorpage.message_key_arg</b><br>\n" +
                "  <br>\n" +
                "  wiki.errorpage.linkToStartpage\n" +
                "</body>";
        assertTrue(response.contains("HTTP/1.1 404 Not Found"));
        assertTrue(response.contains("<title>wiki.errorpage.title | ProgName</title>"));
        assertTrue(response.contains(expectedBody));
        verify(messages, times(1)).getMessage(eq("key"), eq("arg"));
    }

    @Test
    public void testGenerateErrorPageException() throws Exception {
        HttpResponse httpResponse = htmlService.generateErrorPage(500, new ServiceException("error message"), "key", "arg");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpResponse.writeResponse(out);
        String response = out.toString("UTF-8");
        String expectedBody = "<body>\n" +
                "  <b>wiki.errorpage.message_key_arg</b><br>\n" +
                "  net.moasdawiki.base.ServiceException: error message<br>\n" +
                "  <br>\n" +
                "  wiki.errorpage.linkToStartpage\n" +
                "</body>";
        assertTrue(response.contains("HTTP/1.1 500 Internal Server Error"));
        assertTrue(response.contains("<title>wiki.errorpage.title | ProgName</title>"));
        assertTrue(response.contains(expectedBody));
        verify(messages, times(1)).getMessage(eq("key"), eq("arg"));
    }
}
