package io.github.yappy.lua;

/**
 * A runtime error has occurred during execution.
 *
 * This exception can be thrown from {@link LuaFunction#call(Object[])}.
 *
 * @see LuaFunction
 * @author yappy
 */
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
