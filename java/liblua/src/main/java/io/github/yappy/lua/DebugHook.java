package io.github.yappy.lua;

/*
 * Lua -> C -> Java debug hook interface. (package private)
 */
interface DebugHook {

	void hook(int event, int currentline) throws LuaAbortException;

}
