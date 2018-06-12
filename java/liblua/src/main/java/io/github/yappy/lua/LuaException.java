package io.github.yappy.lua;

/**
 * An error occurred at Lua compilation time or runtime.
 * Especially, {@link LuaSyntaxException} means LUA_ERRSYNTAX.
 * {@link LuaRuntimeException} means LUA_ERRRUN.
 * @see LuaSyntaxException
 * @see LuaRuntimeException
 * @author yappy
 */
public class LuaException extends Exception {

	private static final long serialVersionUID = 1L;

	public LuaException() {
	}

	public LuaException(String message) {
		super(message);
	}

	public LuaException(Throwable cause) {
		super(cause);
	}

	public LuaException(String message, Throwable cause) {
		super(message, cause);
	}

}
