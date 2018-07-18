package io.github.yappy.lua.lib;

import io.github.yappy.lua.LuaAbortException;
import io.github.yappy.lua.LuaArg;
import io.github.yappy.lua.LuaFunction;
import io.github.yappy.lua.LuaRuntimeException;

@LuaLibraryTable("sys")
public class SystemFunctions implements LuaLibrary {

	public SystemFunctions() {}

	@Override
	public void close() throws Exception {}

	@LuaLibraryFunction(name = "time", args = {})
	public LuaFunction time = new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaRuntimeException {
			return new Object[] { System.currentTimeMillis() };
		}
	};

	@LuaLibraryFunction(name = "sleep", args = { LuaArg.LONG })
	public LuaFunction sleep = new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaRuntimeException, LuaAbortException {
			long millis = ((Long)args[0]).longValue();
			try {
				Thread.sleep(millis);
			} catch(InterruptedException e) {
				throw new LuaAbortException(e);
			}
			return null;
		}
	};

}
