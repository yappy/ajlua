package io.github.yappy.lua.lib;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

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

	// see: loslib.c - os_date()
	@LuaLibraryFunction(name = "date", args = { LuaArg.STRING, LuaArg.LONG_OR_NIL })
	public LuaFunction date = new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaRuntimeException {
			String format = args[0].toString();
			long time = (args[1] != null) ?
					((Long)args[1]).longValue() : System.currentTimeMillis();

			Calendar cal;
			if (format.startsWith("!")) {
				// UTC
				cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				format = format.substring(1);
			}
			else {
				// local
				cal = Calendar.getInstance();
			}
			cal.setTimeInMillis(time);
			if ("*t".equals(format)) {
				Map<String, Object> table = new HashMap<>();
				// Integer
				table.put("year", cal.get(Calendar.YEAR));
				table.put("month", cal.get(Calendar.MONTH) + 1);
				table.put("day", cal.get(Calendar.DATE));
				table.put("hour", cal.get(Calendar.HOUR_OF_DAY));
				table.put("min", cal.get(Calendar.MINUTE));
				table.put("sec", cal.get(Calendar.SECOND));
				// msec (extension)
				table.put("msec", cal.get(Calendar.MILLISECOND));
				// Sunday is 1
				table.put("wday", cal.get(Calendar.DAY_OF_WEEK));
				table.put("yday", Calendar.DAY_OF_YEAR);
				// Boolean
				table.put("isdst", cal.get(Calendar.DST_OFFSET) != 0);
				return new Object[] { table };
			}
			else {
				// TODO
				return null;
			}
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
