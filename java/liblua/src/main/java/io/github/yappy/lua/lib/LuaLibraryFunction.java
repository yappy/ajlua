package io.github.yappy.lua.lib;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.yappy.lua.LuaArg;
import io.github.yappy.lua.LuaEngine;

/**
 * Annotated field is library function.
 * {@link #name()} is function name.
 * {@link #args()} declares arguments specification.
 * Arguments will be automatically checked and converted following it.
 *
 * @see LuaEngine#openLibrary(LuaLibrary)
 * @see LuaLibraryTable
 * @see LuaArg
 * @author yappy
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface LuaLibraryFunction {

	/**
	 * Function name.
	 * @return Function name.
	 */
	String name();

	/**
	 * Arguments spec.
	 * @return Arguments restriction.
	 */
	LuaArg[] args();

}
