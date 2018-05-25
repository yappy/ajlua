package io.github.yappy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LuaEngine implements AutoCloseable {

	static {
		System.loadLibrary("jlua");
	}

	/** Default memory limit */
	public static final long DEFAULT_MEMORY_LIMIT = 16 * 1024 * 1024;
	/** Default instruction count for interrupt */
	public static final int DEFAULT_INTR_INST_COUNT = 1000;
	/** Lua max stack size. */
	public static final int MAX_STACK = 20;

	// getVersion String[] size
	private static final int VERSION_ARRAY_SIZE	= 4;
	// Function returns multiple values (for C API pcall nresults)
	private static final int LUA_MULTRET		= -1;
	// openLibs() bit
	private static final int LIB_BIT_BASE			= (1 << 0);
	private static final int LIB_BIT_PACKAGE		= (1 << 1);
	private static final int LIB_BIT_COROUTINE		= (1 << 2);
	private static final int LIB_BIT_TABLE			= (1 << 3);
	private static final int LIB_BIT_IO				= (1 << 4);
	private static final int LIB_BIT_OS				= (1 << 5);
	private static final int LIB_BIT_STRING			= (1 << 6);
	private static final int LIB_BIT_MATH			= (1 << 7);
	private static final int LIB_BIT_UTF8			= (1 << 8);
	private static final int LIB_BIT_DEBUG			= (1 << 9);
	private static final int LIB_ID_COUNT			= 10;
	// Lua C API return code (lua.h)
	private static final int LUA_OK					= 0;
	private static final int LUA_YIELD				= 1;
	private static final int LUA_ERRRUN				= 2;
	private static final int LUA_ERRSYNTAX			= 3;
	private static final int LUA_ERRMEM				= 4;
	private static final int LUA_ERRGCMM			= 5;
	private static final int LUA_ERRERR				= 6;
	// Lua C API value type (lua.h)
	private static final int LUA_TNIL				= 0;
	private static final int LUA_TBOOLEAN			= 1;
	private static final int LUA_TLIGHTUSERDATA		= 2;
	private static final int LUA_TNUMBER			= 3;
	private static final int LUA_TSTRING			= 4;
	private static final int LUA_TTABLE				= 5;
	private static final int LUA_TFUNCTION			= 6;
	private static final int LUA_TUSERDATA			= 7;
	private static final int LUA_TTHREAD			= 8;
	// For getCheckedValues()
	private static final int CHECK_TYPE_BOOLEAN		= 0;
	private static final int CHECK_TYPE_INTEGER		= 1;
	private static final int CHECK_TYPE_NUMBER		= 2;
	private static final int CHECK_TYPE_STRING		= 3;
	private static final int CHECK_OPT_ALLOW_NIL	= (1 << 16);
	// Lua C API hook event code (lua.h)
	private static final int LUA_HOOKCALL			= 0;
	private static final int LUA_HOOKRET			= 1;
	private static final int LUA_HOOKLINE			= 2;
	private static final int LUA_HOOKCOUNT			= 3;
	private static final int LUA_HOOKTAILCALL		= 4;
	// Lua C API hook event mask (lua.h)
	private static final int LUA_MASKCALL			= (1 << LUA_HOOKCALL);
	private static final int LUA_MASKRET			= (1 << LUA_HOOKRET);
	private static final int LUA_MASKLINE			= (1 << LUA_HOOKLINE);
	private static final int LUA_MASKCOUNT			= (1 << LUA_HOOKCOUNT);

	// Native interface
	private static native int getVersionInfo(String[] info);
	private static native long newPeer(long nativeMemoryLimit);
	private static native void deletePeer(long peer);
	private static native void setDebugHook(long peer, DebugHook hook);
	private static native void setHookMask(long peer, int mask, int count);
	private static native int openLibs(long peer, int libs);
	private static native int replacePrintFunc(long peer, LuaPrint print);
	private static native int loadString(
			long peer, String buf, String chunkName);
	private static native int getTop(long peer);
	private static native void setTop(long peer, int index);
	private static native int pushValues(long peer, Object[] values);
	private static native int getValues(
			long peer, byte[] types, Object[] values);
	private static native int getCheckedValues(
			long peer, int[] checks, Object[] values);
	private static native int pcall(
			long peer, int nargs, int nresults, int msgh)
			throws InterruptedException;
	private static native int getGlobal(long peer, String name);
	private static native int setGlobal(long peer, String name);
	private static native void setProxyCallback(
			long peer, FunctionRoot callback);
	private static native int pushProxyFunction(long peer, int id);

	// private variables
	private long peer = 0;
	private int versionInt;
	private String version, release, copyright, author;
	private LuaHook hook = null;
	private LuaPrint print = null;
	private LuaPrint printRoot = new LuaPrintImpl();
	private List<LuaFunction> functionList = new ArrayList<LuaFunction>();

	public LuaEngine() {
		this(DEFAULT_MEMORY_LIMIT, DEFAULT_INTR_INST_COUNT);
	}

	public LuaEngine(long nativeMemoryLimit, int intrInstCount) {
		this.peer = newPeer(nativeMemoryLimit);
		if (peer == 0) {
			// probably cannot allocate in native heap
			throw new OutOfMemoryError();
		}
		setDebugHook(peer, new DebugHookImpl());
		setHookMask(peer, LUA_MASKCOUNT, intrInstCount);
		setProxyCallback(peer, new FunctionRootImpl());

		String[] strs = new String[VERSION_ARRAY_SIZE];
		versionInt = getVersionInfo(strs);
		this.version = strs[0];
		this.release = strs[1];
		this.copyright = strs[2];
		this.author = strs[3];
	}

	@Override
	public void close() {
		deletePeer(peer);
		peer = 0;
	}

	public int getVersionInt() {
		return versionInt;
	}
	public String getVersion() {
		return version;
	}
	public String getRelease() {
		return release;
	}
	public String getCopyright() {
		return copyright;
	}
	public String getAuthor() {
		return author;
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

	private Object[] popAndConvertAll() {
		byte[] types = new byte[MAX_STACK];
		Object[] values = new Object[MAX_STACK];
		int count = getValues(peer, types, values);
		setTop(peer, 0);

		Object[] result = new Object[count];
		for (int i = 0; i < result.length; i++) {
			result[i] = covertL2J(types[i], values[i]);
		}
		return result;
	}

	private int pcallWithHook(LuaHook hook, int nargs, int nresults, int msgh)
			throws InterruptedException {
		this.hook = hook;
		try {
			return pcall(peer, nargs, nresults, msgh);
		}
		finally {
			this.hook = null;
		}
	}

	private class DebugHookImpl implements DebugHook {
		@Override
		public void hook(int event, int currentline)
				throws InterruptedException {
			if (hook != null) {
				switch (event) {
				case LUA_HOOKCALL:
					hook.hook(LuaHook.Type.CALL, currentline);
				case LUA_HOOKRET:
					hook.hook(LuaHook.Type.RET, currentline);
				case LUA_HOOKLINE:
					hook.hook(LuaHook.Type.LINE, currentline);
				case LUA_HOOKCOUNT:
					hook.hook(LuaHook.Type.COUNT, currentline);
				case LUA_HOOKTAILCALL:
					hook.hook(LuaHook.Type.TAILCALL, currentline);
				default:
					throw new Error("Unkwon hook event");
				}
			}
			// return true if interrupted
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException();
			}
		}
	}

	private class LuaPrintImpl implements LuaPrint {
		@Override
		public void writeString(String str) {
			if (print != null) {
				print.writeString(str);
			}
			else {
				// default
				System.out.print(str);
			}
		}
		@Override
		public void writeLine() {
			if (print != null) {
				print.writeLine();
			}
			else {
				// default
				System.out.println();
			}
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
			Object[] params = popAndConvertAll();

			// dispatch
			Object[] results = functionList.get(id).call(params);

			// Push results into the stack and return count
			if (results == null || results.length == 0) {
				return 0;
			}
			checkLuaError(pushValues(peer, results));
			return results.length;
		}
	}

	public void openStdLibs() throws LuaException {
		openStdLibs(LuaStdLib.DEFAULT_SET);
	}

	public void openStdLibs(Set<LuaStdLib> libs) throws LuaException {
		int bits = 0;
		for (LuaStdLib e : libs) {
			bits |= e.ordinal();
		}
		checkLuaError(openLibs(peer, bits));

		// replace "print" function if base library is loaded
		if (libs.contains(LuaStdLib.BASE)) {
			replacePrintFunc(peer, printRoot);
		}
	}

	public void setPrintFunction(LuaPrint print) {
		this.print = print;
	}

	public void addGlobalFunction(String name, LuaFunction func)
			throws LuaException {
		if (name == null) {
			throw new NullPointerException("name");
		}
		if (func == null) {
			throw new NullPointerException("func");
		}

		int id = functionList.size();
		functionList.add(func);

		checkLuaError(pushProxyFunction(peer, id));
		checkLuaError(setGlobal(peer, name));
	}

	public void setGlobalVariable(String name, Object value)
			throws LuaException {
		if (name == null) {
			throw new NullPointerException("name");
		}

		checkLuaError(pushValues(peer, new Object[] { value }));
		checkLuaError(setGlobal(peer, name));
	}

	public Object[] callGlobalFunction(
			String name, Object... params)
			throws LuaException, InterruptedException {
		return callGlobalFunction(null, name, params);
	}

	public Object[] callGlobalFunction(
			LuaHook hook, String name, Object... params)
			throws LuaException, InterruptedException {
		if (name == null) {
			throw new NullPointerException("name");
		}
		if (params == null) {
			throw new NullPointerException("params");
		}
		if (getTop(peer) != 0) {
			throw new IllegalStateException("stack not empty");
		}

		// push global
		checkLuaError(getGlobal(peer, name));
		// push parameters
		checkLuaError(pushValues(peer, params));
		// pcall
		checkLuaError(pcallWithHook(hook, params.length, LUA_MULTRET, 0));
		// pop results
		Object[] results = popAndConvertAll();

		return results;
	}


	public long getPeerForDebug() {
		return peer;
	}

	public void execString(String buf, String chunkName)
			throws LuaException, InterruptedException {
		execString(null, buf, chunkName);
	}

	public void execString(LuaHook hook, String buf, String chunkName)
			throws LuaException, InterruptedException {
		// push chunk function
		checkLuaError(loadString(peer, buf, chunkName));
		// pcall nargs=0, nresults=0
		checkLuaError(pcallWithHook(hook, 0, 0, 0));
	}

	private void checkLuaError(int code) throws LuaException {
		switch (code) {
		case LUA_OK:
			return;
		case LUA_ERRRUN:
			throw new LuaRuntimeException("runtime error: " + getLuaErrorMsg());
		case LUA_ERRSYNTAX:
			throw new LuaSyntaxException("syntax error: " + getLuaErrorMsg());
		case LUA_ERRMEM:
			throw new LuaException("memory error");
		case LUA_ERRGCMM:
			throw new LuaException("error in gc");
		case LUA_ERRERR:
			throw new LuaException("error in message handler");
		default:
			throw new Error("Unknown return code: " + code);
		}
	}

	private String getLuaErrorMsg()
	{
		byte[] types = new byte[MAX_STACK];
		Object[] values = new Object[MAX_STACK];
		int num = getValues(peer, types, values);
		if (num <= 0) {
			return "(no message)";
		}
		else if (values[num - 1] == null) {
			return "(invalid message type)";
		}
		else {
			return values[num - 1].toString();
		}
	}

}
