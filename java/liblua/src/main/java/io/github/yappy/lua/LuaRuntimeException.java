package io.github.yappy.lua;

/**
 * A runtime error has occurred during execution.
 * This exception represents LUA_ERRRUN.
 * This error can be caused by bad arithmetic operation,
 * function call for not function value,
 * error() or assert() standard lua functions, etc.
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
