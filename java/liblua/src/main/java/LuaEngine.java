
public class LuaEngine implements AutoCloseable {

	static {
		System.loadLibrary("jlua");
	}

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

	public static final int MIN_STACK = 20;
	public static final int LUA_MULTRET = -1;


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


	private long peer = 0;

	public LuaEngine() {
		this.peer = newPeer();
		if (peer == 0) {
			// probably cannot allocate in native heap
			throw new OutOfMemoryError();
		}
	}

	@Override
	public void close() {
		deletePeer(peer);
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
			throw new Exception("syntax error");
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
			throw new Exception("runtime error");
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

}
