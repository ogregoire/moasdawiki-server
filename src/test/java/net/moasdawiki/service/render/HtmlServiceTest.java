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

package net.moasdawiki.service.render;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Messages;
import net.moasdawiki.base.ServiceException;
import net.moasdawiki.base.Settings;
import net.moasdawiki.service.HttpResponse;
import net.moasdawiki.service.repository.AnyFile;
import net.moasdawiki.service.transform.TransformerService;
import net.moasdawiki.service.wiki.WikiFile;
import net.moasdawiki.service.wiki.WikiService;
import net.moasdawiki.service.wiki.structure.TextOnly;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class HtmlServiceTest {

    private Settings settings;
    private Messages messages;
    private WikiService wikiService;
    private HtmlService htmlService;
    private TransformerService transformerService;

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
        transformerService = mock(TransformerService.class);
        when(transformerService.applyTransformations(any())).thenAnswer(invocation -> invocation.getArgument(0));
        htmlService = new HtmlService(new Logger(null), settings, messages, wikiService, transformerService);
    }

    @Test
    public void testConvertHtmlMinimal() {
        HttpResponse httpResponse = htmlService.convertHtml(new HtmlWriter());
        String response = new String(httpResponse.content, StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
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
        String response = new String(httpResponse.content, StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
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
        String response = new String(httpResponse.content, StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
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
        String response = new String(httpResponse.content, StandardCharsets.UTF_8);
        String expected = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <title>pagePath | ProgName</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  text only\n" +
                "</body>\n" +
                "</html>\n";
        assertEquals(response, expected);
        verify(transformerService, times(1)).applyTransformations(any());
    }

    @Test
    public void testGenerateRedirectToWikiPage() {
        HttpResponse httpResponse = htmlService.generateRedirectToWikiPage("/pagePath");
        assertEquals(302, httpResponse.statusCode);
        assertEquals("/view/pagePath", httpResponse.redirectUrl);
    }

    @Test
    public void testGenerateMessagePage() {
        HttpResponse httpResponse = htmlService.generateMessagePage("key", "arg");
        String responseStr = new String(httpResponse.content, StandardCharsets.UTF_8);
        String expectedBody = "<body>\n" +
                "  <b>key_arg</b>\n" +
                "</body>";
        assertTrue(responseStr.contains(expectedBody));
        verify(messages, times(1)).getMessage(eq("key"), eq("arg"));
    }

    @Test
    public void testGenerateErrorPage() {
        HttpResponse httpResponse = htmlService.generateErrorPage(404, "key", "arg");
        assertEquals(404, httpResponse.statusCode);
        String responseStr = new String(httpResponse.content, StandardCharsets.UTF_8);
        assertTrue(responseStr.contains("<title>wiki.errorpage.title | ProgName</title>"));
        String expectedBody = "<body>\n" +
                "  <b>wiki.errorpage.message_key_arg</b><br>\n" +
                "  <br>\n" +
                "  wiki.errorpage.linkToStartpage\n" +
                "</body>";
        assertTrue(responseStr.contains(expectedBody));
        verify(messages, times(1)).getMessage(eq("key"), eq("arg"));
    }

    @Test
    public void testGenerateErrorPageException() {
        HttpResponse httpResponse = htmlService.generateErrorPage(500, new ServiceException("error message"), "key", "arg");
        assertEquals(500, httpResponse.statusCode);
        String responseStr = new String(httpResponse.content, StandardCharsets.UTF_8);
        String expectedBody = "<body>\n" +
                "  <b>wiki.errorpage.message_key_arg</b><br>\n" +
                "  net.moasdawiki.base.ServiceException: error message<br>\n" +
                "  <br>\n" +
                "  wiki.errorpage.linkToStartpage\n" +
                "</body>";
        assertTrue(responseStr.contains("<title>wiki.errorpage.title | ProgName</title>"));
        assertTrue(responseStr.contains(expectedBody));
        verify(messages, times(1)).getMessage(eq("key"), eq("arg"));
    }
}
