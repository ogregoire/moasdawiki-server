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

package net.moasdawiki;

import org.jetbrains.annotations.NotNull;

/**
 * Main class with static main() method.
 * Is used to start MoasdaWiki from command line.
 */
public class Main {

	@NotNull
	private final MainService mainService;

	private Main() {
		mainService = new MainService();
	}

	private void runServer(String[] args) {
		mainService.init(args);
		mainService.setShutdownRequestAllowed(true);
		printUrls();
		mainService.runBlocking();
	}

	private void printUrls() {
		String localHostName = mainService.getSettings().getServerHost();
		int port = mainService.getSettings().getServerPort();
		String msg = mainService.getMessages().getMessage("wiki.server.url");
		if (mainService.getSettings().isOnlyLocalhostAccess() || localHostName == null) {
			System.out.println(msg + " http://localhost:" + port + '/');
		} else {
			System.out.println(msg);
			System.out.println("  http://localhost:" + port + "/");
			System.out.println("  http://" + localHostName + ":" + port + "/");
		}
	}

	public static void main(String[] args) {
		final Main main = new Main();

		// Stop server on JVM shutdown; this is triggered by Ctrl+C in the terminal.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Received Java VM STOP signal, shutting down server");
			main.mainService.stop();
		}));

		// Server starten
		main.runServer(args);
	}
}
