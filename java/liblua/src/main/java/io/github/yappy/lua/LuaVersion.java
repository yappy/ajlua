package io.github.yappy.lua;

/**
 * Lua version info.
 *
 * @author yappy
 */
public final class LuaVersion {

	public final int VERSION_INT;
	public final String VERSION;
	public final String RELEASE;
	public final String COPYRIGHT;
	public final String AUTHOR;

	public LuaVersion(int versionInt, String version, String release,
			String copyright, String author) {
		VERSION_INT = versionInt;
		VERSION = version;
		RELEASE = release;
		COPYRIGHT = copyright;
		AUTHOR = author;
	}

}
