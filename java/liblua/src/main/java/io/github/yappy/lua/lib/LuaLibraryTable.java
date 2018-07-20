package io.github.yappy.lua.lib;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.yappy.lua.LuaEngine;

/**
 * Annotated class can be loaded by {@link LuaEngine#openLibrary(LuaLibrary)}.
 * {@link #value()} is Lua table variable name for this library.
 * Each function needs to be annotated with {@link LuaLibraryFunction}.
 *
 * @see LuaEngine#openLibrary(LuaLibrary)
 * @see LuaLibraryFunction
 * @author yappy
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface LuaLibraryTable {

	/**
	 * Library table name.
	 * @return Library table name.
	 */
	String value();

}
