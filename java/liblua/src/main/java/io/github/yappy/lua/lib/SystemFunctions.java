package io.github.yappy.lua.lib;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

	// C89 strftime() compatible (do the best)
	// aAbBcdHIjmMpSUwWxXyYZ
	private static final List<DateFormat> CONVERTER;
	static {
		List<DateFormat> tmp = new ArrayList<>(128);
		for (int i = 0; i < 128; i++) {
			tmp.add(null);
		}

		// day of week
		tmp.set('a', new SimpleDateFormat("E"));
		tmp.set('A', new SimpleDateFormat("EEEE"));
		// month
		tmp.set('b', new SimpleDateFormat("MMM"));
		tmp.set('B', new SimpleDateFormat("MMMM"));
		// date + time
		tmp.set('c', DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL));
		// 01-31
		tmp.set('d', new SimpleDateFormat("DD"));
		// TODO

		CONVERTER = Collections.unmodifiableList(tmp);
	}

	private static String convertTime(char c, Calendar cal) throws LuaRuntimeException {
		DateFormat conv = null;
		if (c < CONVERTER.size()) {
			conv = CONVERTER.get(c);
		}
		if (conv == null) {
			throw new LuaRuntimeException("invalid conversion specifier '%" + c + "'");
		}
		conv.setCalendar(cal);
		return conv.format(cal.getTime());
	}

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
				// strftime
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < format.length(); i++) {
					char c = format.charAt(i);
					if (c == '%') {
						i++;
						if (i >= format.length()) {
							throw new LuaRuntimeException("invalid conversion specifier '%'");
						}
						char convSpec = format.charAt(i);
						result.append(convertTime(convSpec, cal));
					}
					else {
						result.append(c);
					}
				}
				return new Object[] { result.toString() };
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
