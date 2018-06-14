package io.github.yappy.lua.lib;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.yappy.lua.LuaArg;

@Documented
@Retention(RUNTIME)
@Target(FIELD)
/**
 * @author yappy
 */
public @interface LuaLibraryFunction {

	String name();

	LuaArg[] args();

}
