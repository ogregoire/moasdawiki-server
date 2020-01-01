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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Zum Festlegen der Aufrufreihenfolge der Plugin-Methode
 * {@link Plugin#transformWikiPage(net.moasdawiki.service.wiki.structure.WikiPage)}. Die
 * Methodenaufrufe erfolgen in aufsteigender Nummernfolge. Wenn mehrere Plugins
 * dieselbe Nummer haben, ist deren Reihenfolge zufällig. Wenn die Annotation
 * fehlt, erfolgt der Aufruf zum Schluss.
 * 
 * @author Herbert Reiter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CallOrder {
	int value();
}
