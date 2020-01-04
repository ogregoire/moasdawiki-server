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

package net.moasdawiki.plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.moasdawiki.base.Logger;
import net.moasdawiki.base.Settings;
import net.moasdawiki.server.HttpRequest;
import net.moasdawiki.service.wiki.structure.WikiPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Schnittstelle zum Zugriff auf alle Plugins. Von jedem Plugin wird genau eine
 * Instanz angelegt.
 * 
 * @author Herbert Reiter
 */
public class PluginService {

	@NotNull
	private final Logger logger;

	@NotNull
	private final Settings settings;

	/**
	 * Geladene Plugins.
	 */
	@NotNull
	private final Set<Plugin> plugins;

	/**
	 * Liste aller Plugins zur Transformation von Wikiseiten. Die Plugins werden
	 * in genau dieser Reihenfolge aufgerufen.
	 */
	@NotNull
	private final List<Plugin> pagetransformationPlugins;

	/**
	 * URL-Mapping.
	 */
	@NotNull
	private final Map<Pattern, Plugin> urlPluginMap;

	/**
	 * Konstruktor.
	 */
	public PluginService(@NotNull Logger logger, @NotNull Settings settings) {
		this.logger = logger;
		this.settings = settings;
		plugins = new HashSet<>();
		pagetransformationPlugins = new ArrayList<>();
		urlPluginMap = new HashMap<>();
	}

	/**
	 * Lädt alle Plugins. Dazu wird der Classpath nach Implementierungen des
	 * Interfaces {@link Plugin} durchsucht und von jeder Klasse eine einzige
	 * Instanz erzeugt.
	 */
	public void loadPlugins(@NotNull ServiceLocator serviceLocator) {
		// reset cache
		plugins.clear();
		pagetransformationPlugins.clear();
		urlPluginMap.clear();

		// lade Plugins
		Set<String> classNames = settings.getPluginClassNames();
		for (String className : classNames) {
			try {
				Class<?> pluginClass = Class.forName(className);
				if (Plugin.class.isAssignableFrom(pluginClass)) {
					Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
					plugin.setServiceLocator(serviceLocator);
					plugins.add(plugin);
					logger.write("Plug-in '" + plugin.getClass().getSimpleName() + "' loaded");
				} else {
					logger.write("Plug-in '" + pluginClass + "' doesn't implement Plugin interface, ignoring it");
				}
			} catch (Exception e) {
				logger.write("Error loading plug-in '" + className + "'", e);
			}
		}

		fillPageTransformationList();
		fillUrlPluginMap();
	}

	private void fillPageTransformationList() {
		// Annotationen der Plugins auslesen
		List<CallOrderEntry> prioList = new ArrayList<>();
		for (Plugin plugin : plugins) {
			try {
				Method method = plugin.getClass().getMethod("transformWikiPage", WikiPage.class);
				CallOrder callOrder = method.getAnnotation(CallOrder.class);

				CallOrderEntry entry = new CallOrderEntry();
				if (callOrder != null) {
					entry.order = callOrder.value();
				} else {
					entry.order = Integer.MAX_VALUE; // zum Schluss
				}
				entry.plugin = plugin;
				prioList.add(entry);
			} catch (Exception e) {
				// kann nicht auftreten
			}
		}

		// Liste sortieren
		prioList.sort(Comparator.comparingInt(o -> o.order));

		// pagetransformationPlugins füllen
		pagetransformationPlugins.clear();
		for (CallOrderEntry entry : prioList) {
			pagetransformationPlugins.add(entry.plugin);
		}
	}

	private void fillUrlPluginMap() {
		for (Plugin plugin : plugins) {
			try {
				// Pattern aus Annotation auslesen
				Method method = plugin.getClass().getMethod("handleRequest", HttpRequest.class);
				PathPattern pathPattern = method.getAnnotation(PathPattern.class);

				Set<String> patternSet = new HashSet<>();
				if (pathPattern != null) {
					pathPattern.value();
					patternSet.add(pathPattern.value());
				}
				if (pathPattern != null) {
					pathPattern.multiValue();
					Collections.addAll(patternSet, pathPattern.multiValue());
				}

				// Pattern parsen
				for (String patternStr : patternSet) {
					try {
						Pattern pattern = Pattern.compile(patternStr);
						urlPluginMap.put(pattern, plugin);
					} catch (Exception e) {
						logger.write("Invalit URL pattern '" + patternStr + "' in plug-in '" + plugin.getClass().getSimpleName() + "'!");
					}
				}
			} catch (Exception e) {
				// kann nicht auftreten
			}
		}
	}

	/**
	 * Gibt die Menge aller vorhandenen Plugins zurück. Hierin sind alle Plugins
	 * unabhängig von ihrer Verwendung enthalten.
	 * 
	 * @return Nicht <code>null</code>.
	 */
	@SuppressWarnings("unused")
	@NotNull
	public Set<Plugin> getPlugins() {
		return Collections.unmodifiableSet(plugins);
	}

	/**
	 * Gibt das Plugin zurück, das gemäß URL-Mapping für die angegebene URL
	 * zuständig ist.
	 * 
	 * @param urlPath Pfad in einer URL. Nicht <code>null</code>.
	 * @return Das Plugin. <code>null</code> -> kein solches Plugin vorhanden.
	 */
	@Nullable
	public Plugin getPluginByUrl(@NotNull String urlPath) {
		for (Pattern pattern : urlPluginMap.keySet()) {
			if (pattern.matcher(urlPath).matches()) {
				return urlPluginMap.get(pattern);
			}
		}

		// kein passendes Plugin gefunden
		return null;
	}

	/**
	 * Wendet alle Plugins auf die angegebene Wikiseite an und transformiert
	 * sie.
	 * 
	 * @param wikiPage Die originale Wikiseite. Darf niemals modifiziert werden!
	 *        Nicht <code>null</code>.
	 * @return Transformierte Wikiseite. Nicht <code>null</code>.
	 */
	@NotNull
	public WikiPage applyTransformations(@NotNull WikiPage wikiPage) {
		for (Plugin plugin : pagetransformationPlugins) {
			wikiPage = plugin.transformWikiPage(wikiPage);
		}
		return wikiPage;
	}

	/**
	 * Enthält die Aufrufpriorität für ein Plugin.
	 */
	private static class CallOrderEntry {
		public int order;
		public Plugin plugin;
	}
}
