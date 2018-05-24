package io.github.yappy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum LuaStdLib {
	BASE, PACKAGE, COROUTINE, TABLE, IO,
	OS, STRING, MATH, UTF8, DEBUG;

	public static final Set<LuaStdLib> DEFAULT_SET =
		Collections.unmodifiableSet(EnumSet.of(
			BASE, COROUTINE, TABLE, IO, STRING, MATH, UTF8));
}
