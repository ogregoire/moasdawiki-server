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

package net.moasdawiki.service.wiki;

import net.moasdawiki.service.wiki.structure.PageElement;
import org.jetbrains.annotations.NotNull;

/**
 * Callback method to traverse a wiki tree.
 * <p>
 * The method must not modify the wiki tree.
 * To modify a wiki tree see {@link PageElementTransformer}.
 *
 * @param <T> PageElement sub-type that is to traverse
 * @param <C> type of the context object
 * @author Herbert Reiter
 */
@FunctionalInterface
public interface PageElementConsumer<T extends PageElement, C> {

    /**
     * Comsumer method that is called for all matching tree nodes.
     *
     * @param pageElement A tree node. Must not be modified!
     * @param context     Context object to share data and pass the result.
     */
    void consume(@NotNull T pageElement, @NotNull C context);
}
