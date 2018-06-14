package io.github.yappy.lua;

/**
 * Type check and conversion rule for Lua -&gt; Java function call arguments.
 * @see LuaFunction
 * @see LuaEngine#addGlobalFunction(String, LuaFunction, LuaArg...)
 * @see LuaEngine#addLibFunction(String, String, LuaFunction, LuaArg...)
 * @author yappy
 */
public enum LuaArg {

	/** No check and conversion. */
	ANY(false, true, false),
	/** Covert to Lua boolean and get as Java Boolean. */
	BOOLEAN(false, false, false),
	/** Covert to Lua boolean and get as Java Boolean. (nullable) */
	BOOLEAN_OR_NIL(true, false, false),
	/** Covert to Lua boolean and get as Java Boolean. (0 or any number of elements) */
	BOOLEAN_VAR_ARGS(false, false, true),
	/** Convert to Lua integer and get as Java Long. */
	LONG(false, false, false),
	/** Convert to Lua integer and get as Java Long. (nullable) */
	LONG_OR_NIL(true, false, false),
	/** Convert to Lua integer and get as Java Long. (0 or any number of elements) */
	LONG_VAR_ARGS(false, false, true),
	/** Convert to Lua number and get as Java Double. */
	DOUBLE(false, false, false),
	/** Convert to Lua number and get as Java Double. (nullable) */
	DOUBLE_OR_NIL(true, false, false),
	/** Convert to Lua number and get as Java Double. (0 or any number of elements) */
	DOUBLE_VAR_ARGS(false, false, true),
	/** Convert to Lua string and get as Java String. */
	STRING(false, false, false),
	/** Convert to Lua string and get as Java String. (nullable) */
	STRING_OR_NIL(true, false, false),
	/** Convert to Lua string and get as Java String. (0 or any number of elements) */
	STRING_VAR_ARGS(false, false, true),
	;

	private boolean nullable;
	private boolean any;
	private boolean varArgs;

	private LuaArg(boolean nullable, boolean any, boolean varArgs) {
		this.nullable = nullable;
		this.any = any;
		this.varArgs = varArgs;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isAny() {
		return any;
	}

	public boolean isVarArgs() {
		return varArgs;
	}

}
