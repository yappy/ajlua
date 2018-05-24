package io.github.yappy;

public class LuaRuntimeException extends LuaException {

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
