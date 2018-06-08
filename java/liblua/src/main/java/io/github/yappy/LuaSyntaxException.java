	package io.github.yappy;

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
