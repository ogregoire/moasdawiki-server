/*
 * Copyright (c) 2008 - 2019 Dr. Herbert Reiter (support@moasdawiki.net)
 * 
 * This file is part of MoasdaWiki.
 * 
 * MoasdaWiki is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * MoasdaWiki is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MoasdaWiki. If not, see <http://www.gnu.org/licenses/>.
 */

package net.moasdawiki;

import org.jetbrains.annotations.NotNull;

/**
 * Hauptklasse mit main()-Methode. Wird verwendet, wenn MoasdaWiki von der
 * Kommandozeile aufgerufen wird.
 * 
 * @author Herbert Reiter
 */
public class Main {

	// Service-Schnittstelle des Servers
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

	/**
	 * main-Methode, um die Anwendung zu starten.
	 */
	public static void main(String[] args) {
		final Main main = new Main();

		// wenn die VM heruntergefahren werden soll, dann rechtzeitig den Server
		// beenden; das Event wird durch Strg+C in der Konsole und durch das
		// Ende der main()-Methode ausgelöst
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Received Java VM STOP signal, shutting down server");
			main.mainService.stop();
		}));

		// Server starten
		main.runServer(args);
	}
}
