package io.github.yappy.lua;

/**
 * A syntax error has occurred during compilation.
 * This exception represents LUA_ERRSYNTAX.
 * @author yappy
 */
public class LuaSyntaxException extends LuaException {

	private static final long serialVersionUID = 1L;

	public LuaSyntaxException() {
	}

	public LuaSyntaxException(String message) {
		super(message);
	}

	public LuaSyntaxException(Throwable cause) {
		super(cause);
	}

	public LuaSyntaxException(String message, Throwable cause) {
		super(message, cause);
	}

}
