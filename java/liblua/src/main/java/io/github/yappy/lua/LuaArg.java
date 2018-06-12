package io.github.yappy.lua;

/**
 * Type check and conversion rule for Lua -&gt; Java function call arguments.
 * @see LuaFunction
 * @see LuaEngine#addGlobalFunction(String, LuaFunction, LuaArg...)
 * @see LuaEngine#addLibFunction(String, String, LuaFunction, LuaArg...)
 * @author yappy
 */
public enum LuaArg {

	/** No check. */
	ANY,
	/** Covert to Lua boolean and get as Java Boolean. */
	BOOLEAN,
	/** Covert to Lua boolean and get as Java Boolean. (nullable) */
	BOOLEAN_OR_NIL,
	/** Convert to Lua integer and get as Java Long. */
	LONG,
	/** Convert to Lua integer and get as Java Long. (nullable) */
	LONG_OR_NIL,
	/** Convert to Lua number and get as Java Double. */
	DOUBLE,
	/** Convert to Lua number and get as Java Double. (nullable) */
	DOUBLE_OR_NIL,
	/** Convert to Lua string and get as Java String. */
	STRING,
	/** Convert to Lua string and get as Java String. (nullable) */
	STRING_OR_NIL,

}
