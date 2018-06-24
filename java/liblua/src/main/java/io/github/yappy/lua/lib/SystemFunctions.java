package io.github.yappy.lua.lib;

import io.github.yappy.lua.LuaArg;
import io.github.yappy.lua.LuaException;
import io.github.yappy.lua.LuaFunction;

@LuaLibraryTable("sys")
public class SystemFunctions implements LuaLibrary {

	public SystemFunctions() {}

	@Override
	public void close() throws Exception {}

	@LuaLibraryFunction(name = "time", args = {})
	public LuaFunction time = new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			return new Object[] { System.currentTimeMillis() };
		}
	};

	@LuaLibraryFunction(name = "sleep", args = { LuaArg.LONG })
	public LuaFunction sleep = new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException, InterruptedException {
			long millis = ((Long)args[0]).longValue();
			Thread.sleep(millis);
			return null;
		}
	};

}
