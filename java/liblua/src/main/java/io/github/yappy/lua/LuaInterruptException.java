package io.github.yappy.lua;

/**
 * A runtime error has occurred during execution.
 *
 * This exception represents native result = LUA_ERRRUN.
 * This error can be caused by bad arithmetic operation,
 * function call for not function value,
 * error() or assert() standard lua functions, etc.
 * Also, it can be thrown from Java function called from Lua.
 * It can be caught in Lua code with pcall().
 *
 * @see LuaFunction
 * @author yappy
 */
public class LuaInterruptException extends LuaException {

	private static final long serialVersionUID = 1L;

	public LuaInterruptException() {
	}

	public LuaInterruptException(String message) {
		super(message);
	}

	public LuaInterruptException(Throwable cause) {
		super(cause);
	}

	public LuaInterruptException(String message, Throwable cause) {
		super(message, cause);
	}

}
