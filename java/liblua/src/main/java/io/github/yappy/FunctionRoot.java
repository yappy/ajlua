package io.github.yappy;

/*
 * Lua -> C -> Java function call root interface. (package private)
 */
interface FunctionRoot {

	// @param id ID upvalue associated with the proxy C function.
	// @returns Results count on the stack.
	// @throws Exception Its message will be converted to lua error.
	int call(int id) throws Exception;

}
