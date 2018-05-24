package io.github.yappy;

/*
 * Lua -> C -> Java debug hook interface. (package private)
 */
interface DebugHook {

	// If an exception is thrown, lua error will be occured.
	// @return Raise lua error for aborting pcall if false.
	boolean hook(int event, int currentline);

}
