package io.github.yappy.lua;

/**
 * This exception can be thrown from {@link LuaFunction#call(Object[])}.
 * It raises Lua error and take a jump, but Lua execution cannot continue.
 * If Lua error is caught at pcall() point, take a jump again
 * unless returning to the Java code.
 *
 * @author yappy
 */
public class LuaAbortException extends LuaException {

	private static final long serialVersionUID = 1L;

	public LuaAbortException() {
	}

	public LuaAbortException(String message) {
		super(message);
	}

	public LuaAbortException(Throwable cause) {
		super(cause);
	}

	public LuaAbortException(String message, Throwable cause) {
		super(message, cause);
	}

}
