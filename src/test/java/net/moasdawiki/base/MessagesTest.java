/*
 * MoasdaWiki Server
 * Copyright (C) 2008 - 2021 Herbert Reiter (herbert@moasdawiki.net)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as published
 * by the Free Software Foundation (GPL-3.0-only).
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.html>.
 */

package net.moasdawiki.base;

import net.moasdawiki.service.repository.RepositoryService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

public class MessagesTest {

    private static final String MESSAGES_FILE_CONTENT =
            "# comment\n" +
            "keyWithoutValue\n" +
            "key1=text1\n" +
            "wiki.messageformat.locale=de\n";

    private Messages messages;

    @BeforeMethod
    public void setUp() throws Exception {
        Logger logger = new Logger(null);

        Settings settings = mock(Settings.class);
        when(settings.getMessageFile()).thenReturn("not null");

        RepositoryService repositoryServiceMessages = mock(RepositoryService.class);
        when(repositoryServiceMessages.readTextFile(any())).thenReturn(MESSAGES_FILE_CONTENT);
        messages = new Messages(logger, settings, repositoryServiceMessages);
    }

    @Test
    public void testGetMessage() {
        assertEquals(messages.getMessage("key1"), "text1");
        assertEquals(messages.getMessage("unknownKey"), "unknownKey");
        assertEquals(messages.getMessage("unknown.key"), "key");
    }
}
