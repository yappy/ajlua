package io.github.yappy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

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

	public static final Set<LuaStdLib> DEFAULT_SET =
		Collections.unmodifiableSet(EnumSet.of(
			BASE, COROUTINE, TABLE, IO, STRING, MATH, UTF8));
}
