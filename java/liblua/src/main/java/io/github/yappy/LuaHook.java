package io.github.yappy;

public interface LuaHook {

	public static enum Type {
		CALL, RET, LINE, COUNT, TAILCALL,
	}

	/**
	 *
	 * @param type Hook type.
	 * @param line Line number. (valid only if type == Type.LINE)
	 * @return If false, abort pcall with lua error.
	 */
	boolean hook(Type type, int line);

}
