package io.github.yappy;

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
