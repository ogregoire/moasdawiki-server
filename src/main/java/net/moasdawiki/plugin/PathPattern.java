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

package net.moasdawiki.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Legt über einen regulären Ausdruck fest, für welche URL-Pfade die Methode
 * {@link Plugin#handleRequest(net.moasdawiki.server.HttpRequest)} aufgerufen
 * werden soll. Gibt es mehrere Plugins, deren regulärer Ausdruck auf einen
 * URL-Pfad passt, wird zufällig eines ausgewählt. Wird ein Leerstring bzw. ein
 * leeres Array angegeben, wird die Annotation ignoriert, hat also keinen
 * Effekt.
 * 
 * @author Herbert Reiter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PathPattern {
	String value() default "";

	String[] multiValue() default {};
}
