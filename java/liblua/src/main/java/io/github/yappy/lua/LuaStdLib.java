package io.github.yappy.lua;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lua standard library enum.
 * This enum will be used with EnumSet.
 * @see LuaEngine#openStdLibs(Set)
 * @author yappy
 */
public enum LuaStdLib {
	BASE(0), PACKAGE(1), COROUTINE(2), TABLE(3), IO(4),
	OS(5), STRING(6), MATH(7), UTF8(8), DEBUG(9);

	private int id;

	private LuaStdLib(int id) {
		this.id = id;
	}

	// package private
	int getId() {
		return id;
	}

	/**
	 * Default library set.
	 * This is unmodifiable set of EnumSet.
	 * @see #BASE
	 * @see #COROUTINE
	 * @see #TABLE
	 * @see #STRING
	 * @see #MATH
	 * @see #UTF8
	 */
	public static final Set<LuaStdLib> DEFAULT_SET =
		Collections.unmodifiableSet(EnumSet.of(
			BASE, COROUTINE, TABLE, STRING, MATH, UTF8));
}
