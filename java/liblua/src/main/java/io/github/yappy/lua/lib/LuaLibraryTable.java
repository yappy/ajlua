package io.github.yappy.lua.lib;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
/**
 * @author yappy
 */
public @interface LuaLibraryTable {

	/** Library table name. */
	String value();

}
