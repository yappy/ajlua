package io.github.yappy.lua.lib;

import io.github.yappy.lua.LuaEngine;
import io.github.yappy.lua.LuaFunction;

/**
 * Lua library table and functions.
 * Its object can be loaded by {@link io.github.yappy.lua.LuaEngine#openLibrary(LuaLibrary)}.
 * <ul>
 * <li>Library class must implement this interface.</li>
 * <li>Library class must be annotated with {@link LuaLibraryTable}.</li>
 * <li>Each field for library function must be {@link LuaFunction} type and
 * must be annotated with {@link LuaLibraryFunction}.</li>
 * </ul>
 * The library is {@link AutoCloseable}; its {@link #close()} will be called when
 * {@link LuaEngine} is closing.
 *
 * @see io.github.yappy.lua.LuaEngine#openLibrary(LuaLibrary)
 * @author yappy
 */
public interface LuaLibrary extends AutoCloseable {

}
