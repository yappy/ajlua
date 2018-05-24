package io.github.yappy;

/*
 * Lua -> C -> Java debug hook interface. (package private)
 */
interface DebugHook {

	// If an exception is thrown, lua error will be occured.
	// @throws InterruptedException Causes lua error for aborting pcall.
	boolean hook(int event, int currentline) throws InterruptedException;

}
