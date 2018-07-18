package io.github.yappy.lua;

/**
 * A runtime error has occurred during execution.
 *
 * This exception represents native result = LUA_ERRRUN.
 * It means Lua error is caught.
 * Lua error can be caused by bad arithmetic operation,
 * function call for not function value,
 * error() or assert() standard Lua functions, etc.
 *
 * Also, this exception can be thrown from {@link LuaFunction#call(Object[])}
 * when you want to raise a Lua error from Java code.
 * It will be converted to Lua error with its exception message string.
 *
 * @see LuaFunction
 * @see LuaEngine
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
