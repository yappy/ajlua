
public class LuaEngine implements AutoCloseable {

	static {
		System.loadLibrary("jlua");
	}

	public static native int getVersionInfo(String[] info);
	private static native long newPeer();
	private static native void deletePeer(long peer);


	private long peer = 0;

	public LuaEngine() {
		this.peer = newPeer();
		if (peer == 0) {
			// probably cannot allocate in native heap
			throw new OutOfMemoryError();
		}
	}

	public long getPeerForDebug() {
		return peer;
	}

	@Override
	public void close() {
		deletePeer(peer);
	}

}
