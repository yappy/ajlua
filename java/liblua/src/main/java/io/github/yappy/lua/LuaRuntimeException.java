package io.github.yappy.lua;

public class LuaRuntimeException extends LuaException {

	private static final long serialVersionUID = 1L;

	public LuaRuntimeException() {
	}

	public LuaRuntimeException(String message) {
		super(message);
	}

	public LuaRuntimeException(Throwable cause) {
		super(cause);
	}

	public LuaRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
