package io.github.yappy;

import java.util.ArrayList;
import java.util.List;

public class LuaEngine implements AutoCloseable {

	static {
		System.loadLibrary("jlua");
	}

	/** Lua max stack size. */
	public static final int MAX_STACK = 20;
	/** Function returns multiple values (for pcall nresults) */
	public static final int LUA_MULTRET = -1;


	// lua.h
	private static final int LUA_OK			= 0;
	private static final int LUA_YIELD		= 1;
	private static final int LUA_ERRRUN		= 2;
	private static final int LUA_ERRSYNTAX	= 3;
	private static final int LUA_ERRMEM		= 4;
	private static final int LUA_ERRGCMM	= 5;
	private static final int LUA_ERRERR		= 6;

	private static final int LUA_TNIL			= 0;
	private static final int LUA_TBOOLEAN		= 1;
	private static final int LUA_TLIGHTUSERDATA	= 2;
	private static final int LUA_TNUMBER		= 3;
	private static final int LUA_TSTRING		= 4;
	private static final int LUA_TTABLE			= 5;
	private static final int LUA_TFUNCTION		= 6;
	private static final int LUA_TUSERDATA		= 7;
	private static final int LUA_TTHREAD		= 8;


	public static native int getVersionInfo(String[] info);
	private static native long newPeer();
	private static native void deletePeer(long peer);
	private static native int loadString(
		long peer, String buf, String chunkName);
	public static native int getTop(long peer);
	public static native void setTop(long peer, int index);
	public static native int pushValues(long peer, Object[] values);
	public static native int getValues(
		long peer, byte[] types, Object[] values);
	private static native int pcall(
		long peer, int nargs, int nresults, int msgh);
	public static native int getGlobal(long peer, String name);
	public static native int setGlobal(long peer, String name);
	private static native void setProxyCallback(
		long peer, FunctionRoot callback);
	private static native int pushProxyFunction(long peer, int id);


	private long peer = 0;
	private int nextFunctionId = 0;
	private List<LuaFunction> functionList = new ArrayList<LuaFunction>();

	public LuaEngine() {
		this.peer = newPeer();
		if (peer == 0) {
			// probably cannot allocate in native heap
			throw new OutOfMemoryError();
		}
		setProxyCallback(peer, new FunctionRootImpl());
	}

	@Override
	public void close() {
		deletePeer(peer);
	}

	private static Object covertL2J(byte type, Object luaValue) {
		switch(type) {
		case LUA_TNIL:
			return null;
		case LUA_TBOOLEAN:
		case LUA_TNUMBER:
		case LUA_TSTRING:
			return luaValue;
		default:
			return null;
		}
	}

	// Function call root
	private class FunctionRootImpl implements FunctionRoot {
		@Override
		public int call(int id) throws Exception {
			if (id < 0 || id >= functionList.size()) {
				throw new Error("Invalid function root call ID");
			}

			// Pop all params from the stack
			byte[] types = new byte[MAX_STACK];
			Object[] values = new Object[MAX_STACK];
			int numParams = getValues(peer, types, values);
			setTop(peer, 0);

			Object[] params = new Object[numParams];
			for (int i = 0; i < params.length; i++) {
				params[i] = covertL2J(types[i], values[i]);
			}

			// dispatch
			Object[] results = functionList.get(id).call(params);

			// Push results into the stack and return count
			if (results == null) {
				return 0;
			}
			pushValues(peer, results);
			return results.length;
		}
	}

	public void addGlobalFunction(String name, LuaFunction func) {
		if (func == null) {
			throw new NullPointerException("func");
		}
		if (name == null) {
			throw new NullPointerException("name");
		}

		int id = functionList.size();
		functionList.add(func);
		// TODO: error handling
		pushProxyFunction(peer, id);
		setGlobal(peer, name);
	}


	public long getPeerForDebug() {
		return peer;
	}

	public void loadString(String buf, String chunkName) throws Exception {
		int ret = loadString(peer, buf, chunkName);
		// TODO: define exception
		switch (ret) {
		case LUA_OK:
			return;
		case LUA_ERRSYNTAX:
			throw new Exception("syntax error: " + getErrorMessage());
		case LUA_ERRMEM:
			throw new OutOfMemoryError();
		case LUA_ERRGCMM:
			throw new Exception("error in gc");
		default:
			throw new Error();
		}
	}

	public void pcall(int nargs, int nresults) throws Exception {
		int ret = pcall(peer, nargs, nresults, 0);
		// TODO: define exception
		switch (ret) {
		case LUA_OK:
			return;
		case LUA_ERRRUN:
			throw new Exception("runtime error: " + getErrorMessage());
		case LUA_ERRMEM:
			throw new OutOfMemoryError();
		case LUA_ERRERR:
			throw new Exception("error in msgh");
		case LUA_ERRGCMM:
			throw new Exception("error in gc");
		default:
			throw new Error();
		}
	}

	private String getErrorMessage()
	{
		byte[] types = new byte[MAX_STACK];
		Object[] values = new Object[MAX_STACK];
		int num = getValues(peer, types, values);
		if (num <= 0) {
			return "";
		}
		else if (values[num - 1] == null) {
			return "";
		}
		else {
			return values[num - 1].toString();
		}
	}

}
