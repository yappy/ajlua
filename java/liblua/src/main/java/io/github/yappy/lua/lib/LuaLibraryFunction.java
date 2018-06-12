package io.github.yappy.lua.lib;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.yappy.lua.LuaArg;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
/**
 * @author yappy
 */
public @interface LuaLibraryFunction {

	String name();

	LuaArg[] args();

}
