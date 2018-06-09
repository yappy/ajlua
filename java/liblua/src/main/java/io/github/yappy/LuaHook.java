package io.github.yappy;

/**
 * Lua debug hook interface.
 * @author yappy
 */
public interface LuaHook {

	/**
	 * Debug hook callback reason.
	 * @author yappy
	 */
	public static enum Type {
		/** A function is called. */
		CALL,
		/** A function is returned. */
		RET,
		/** A line will be executed now. */
		LINE,
		/** Instruction counter is reached. */
		COUNT,
		/** A function is called as tail call. RET event will not happen. */
		TAILCALL,
	}

	/**
	 * Hook callback.
	 * @param type Hook type.
	 * @param line Line number. (valid only if type == {@link Type#LINE})
	 */
	void hook(Type type, int line);

}
