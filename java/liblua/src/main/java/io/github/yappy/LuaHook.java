package io.github.yappy;

public interface LuaHook {

	public static enum Type {
		CALL, RET, LINE, COUNT, TAILCALL,
	}

	/**
	 *
	 * @param type Hook type.
	 * @return If true, abort pcall with lua error.
	 */
	boolean hook(Type type, int line);

}
