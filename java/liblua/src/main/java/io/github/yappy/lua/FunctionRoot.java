package io.github.yappy.lua;

/*
 * Lua -> C -> Java function call root interface. (package private)
 */
interface FunctionRoot {

	// @param id ID upvalue associated with the proxy C function.
	// @returns Results count on the stack.
	// @throws LuaRuntimeException Its message will be converted to lua error.
	// @throws Other_throwable(including Error and RuntimeException)
	// Lua execution will be aborted by lua error,
	// but Java exception is still active at pcall() return.
	int call(int id) throws LuaRuntimeException, LuaException, InterruptedException;

}
