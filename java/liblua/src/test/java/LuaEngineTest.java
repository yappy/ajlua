import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import io.github.yappy.lua.LuaAbortException;
import io.github.yappy.lua.LuaArg;
import io.github.yappy.lua.LuaEngine;
import io.github.yappy.lua.LuaException;
import io.github.yappy.lua.LuaFunction;
import io.github.yappy.lua.LuaPrint;
import io.github.yappy.lua.LuaRuntimeException;
import io.github.yappy.lua.LuaSyntaxException;

public class LuaEngineTest {

	private LuaEngine lua;

	@Before
	public void init() throws Exception {
		lua = new LuaEngine();
	}

	@After
	public void term() throws Exception {
		lua.close();
		lua = null;
	}

	@Rule
	public ExpectedException exception = ExpectedException.none();
	@Rule
	public Timeout globalTimeout = Timeout.millis(1000);


	@Test
	public void getVersion() {
		System.out.print("Library Version: ");
		System.out.println(LuaEngine.getLibraryVersion());
	}

	@Test
	public void simpleExecute() throws Exception {
		lua.execString("x = 1", "simpleExecute.lua");
	}

	@Test
	public void syntaxError() throws Exception {
		exception.expect(LuaSyntaxException.class);
		lua.execString("a b c", "syntaxError.lua");
	}

	@Test
	public void runtimeError() throws Exception {
		exception.expect(LuaRuntimeException.class);
		lua.execString("x = nil x(0)", "runtimeError.lua");
	}

	@Test
	public void memoryError() throws Exception {
		exception.expect(LuaException.class);
		exception.expectMessage("memory error");
		lua.execString(
			"t = {}\n" +
			"t[1] = \"abcde\"\n" +
			"i = 2\n" +
			"while true do\n" +
			"  t[i] = t[i - 1] .. t[i - 1]\n" +
			"  i = i + 1\n" +
			"end",
			"runtimeError.lua");
	}

	@Test
	public void timeoutError() throws Exception {
		exception.expect(LuaAbortException.class);
		final Thread main = Thread.currentThread();
		Thread sub = new Thread(new Runnable() {
			@Override
			public void run() {
				try { Thread.sleep(100); } catch (Exception e) {}
				main.interrupt();
			}
		});
		sub.start();
		try {
			lua.execString("while true do end", "timeoutError.lua");
		}
		finally {
			sub.join();
		}
	}

	@Test
	public void stdlibTest() throws Exception {
		lua.openStdLibs();
		lua.execString(
			"function f()\n" +
			"  local t = {5, 2, 1, 4, 3}\n" +
			"  table.sort(t)\n" +
			"  return table.unpack(t)\n" +
			"end",
			"stdlibTest.lua");
		Object[] results = lua.callGlobalFunction("f");
		assertArrayEquals(
			new Object[] { Double.valueOf(1.0), Double.valueOf(2.0),
				Double.valueOf(3.0), Double.valueOf(4.0), Double.valueOf(5.0)},
			results);
	}

	@Test
	public void globalVariable() throws Exception {
		double x = 10007;
		lua.addGlobalVariable("x", x);
		lua.execString("y = x * 2", "globalVariable.lua");
		Object obj = lua.getGlobalVariable("y");
		assertThat(obj, instanceOf(Double.class));
		assertThat((Double)obj, is(x * 2));
	}

	@Test
	public void globalArrayVariable() throws Exception {
		Object[] a = new Object[1024];
		for (int i = 0; i < a.length; i++) {
			// Integer
			a[i] = i + 1;
		}
		lua.openStdLibs();
		lua.addGlobalVariable("a", a);
		lua.execString(
				String.format("assert(#a == %d)\n", a.length) +
				"for i = 1, #a do\n" +
				"  assert(a[i] == i)\n" +
				"end\n" +
				"for i, value in ipairs(a) do\n" +
				"  assert(value == i)\n" +
				"end\n",
				"globalArrayVariable.lua");
	}

	@Test
	public void globalTableVariable() throws Exception {
		Map<String, String> t = new HashMap<>();
		t.put("abc", "XYZ");
		t.put("xyz", "ABC");

		lua.openStdLibs();
		lua.addGlobalVariable("t", t);
		lua.execString(
				"assert(t['abc'] == 'XYZ')\n" +
				"assert(t['xyz'] == 'ABC')\n",
				"globalTableVariable.lua");
	}

	@Test
	public void maxArrayDimension() throws Exception {
		final String value = "hello";

		Object a = new String[][][][][] {{{{{value}}}}};

		lua.openStdLibs();
		lua.addGlobalVariable("a", a);
		lua.execString(
				"while type(a) == \"table\" do\n" +
				"  a = a[1]\n" +
				"end\n" +
				String.format("assert(a == \"%s\")\n", value),
				"maxArrayDimension.lua");
	}

	@Test
	public void libVariable() throws Exception {
		double x = 10007;
		lua.addLibTable("lib");
		lua.addLibVariable("lib", "x", x);
		lua.execString("y = lib.x * 2", "libVariable.lua");
		Object obj = lua.getGlobalVariable("y");
		assertThat(obj, instanceOf(Double.class));
		assertThat((Double)obj, is(x * 2));
	}

	@Test
	public void callGlobalFunction() throws Exception {
		final boolean[] flag = new boolean[1];
		LuaFunction func = new LuaFunction() {
			@Override
			public Object[] call(Object[] args) throws LuaRuntimeException {
				assertThat(args[0], instanceOf(Boolean.class));
				assertThat(((Boolean)args[0]).booleanValue(), is(true));
				assertThat(args[1], instanceOf(Long.class));
				assertThat(((Long)args[1]).longValue(), is(7L));
				assertThat(args[2], instanceOf(Double.class));
				assertThat(((Double)args[2]).doubleValue(), is(3.14));
				assertThat(args[3], instanceOf(String.class));
				assertThat(((String)args[3]), is("hello"));
				flag[0] = true;
				return null;
			}
		};
		lua.addGlobalFunction("func", func,
			LuaArg.BOOLEAN, LuaArg.LONG, LuaArg.DOUBLE, LuaArg.STRING);
		lua.execString("func(true, 7, 3.14, \"hello\")",
			"callGlobalFunction.lua");
		assertTrue(flag[0]);
	}

	@Test
	public void callLibFunction() throws Exception {
		final boolean[] flag = new boolean[1];
		LuaFunction func = new LuaFunction() {
			@Override
			public Object[] call(Object[] args) throws LuaRuntimeException {
				assertThat(args[0], instanceOf(Boolean.class));
				assertThat(((Boolean) args[0]).booleanValue(), is(true));
				assertThat(args[1], instanceOf(Long.class));
				assertThat(((Long) args[1]).longValue(), is(7L));
				assertThat(args[2], instanceOf(Double.class));
				assertThat(((Double) args[2]).doubleValue(), is(3.14));
				assertThat(args[3], instanceOf(String.class));
				assertThat(((String) args[3]), is("hello"));
				flag[0] = true;
				return null;
			}
		};
		lua.addLibTable("lib");
		lua.addLibFunction("lib", "func", func,
			LuaArg.BOOLEAN, LuaArg.LONG, LuaArg.DOUBLE, LuaArg.STRING);
		lua.execString("lib.func(true, 7, 3.14, \"hello\")",
			"callLibFunction.lua");
		assertTrue(flag[0]);
	}

	@Test
	public void callNullableGlobalFunction() throws Exception {
		final boolean[] flag = new boolean[1];
		LuaFunction func = new LuaFunction() {
			@Override
			public Object[] call(Object[] args) throws LuaRuntimeException {
				assertThat(args[0], is(nullValue()));
				assertThat(args[1], is(nullValue()));
				assertThat(args[2], is(nullValue()));
				assertThat(args[3], is(nullValue()));
				flag[0] = true;
				return null;
			}
		};
		lua.addGlobalFunction("func", func,
			LuaArg.BOOLEAN_OR_NIL, LuaArg.LONG_OR_NIL,
			LuaArg.DOUBLE_OR_NIL, LuaArg.STRING_OR_NIL);
		lua.execString("func(nil, nil, nil, nil)",
			"callNullableGlobalFunction.lua");
		assertTrue(flag[0]);
	}

	@Test
	public void callNullableLibFunction() throws Exception {
		final boolean[] flag = new boolean[1];
		LuaFunction func = new LuaFunction() {
			@Override
			public Object[] call(Object[] args) throws LuaRuntimeException {
				assertThat(args[0], is(nullValue()));
				assertThat(args[1], is(nullValue()));
				assertThat(args[2], is(nullValue()));
				assertThat(args[3], is(nullValue()));
				flag[0] = true;
				return null;
			}
		};
		lua.addLibTable("lib");
		lua.addLibFunction("lib", "func", func,
			LuaArg.BOOLEAN_OR_NIL, LuaArg.LONG_OR_NIL,
			LuaArg.DOUBLE_OR_NIL, LuaArg.STRING_OR_NIL);
		lua.execString("lib.func(nil, nil, nil, nil)",
			"callNullableLibFunction.lua");
		assertTrue(flag[0]);
	}

	@Test
	public void callVerArgsFunction() throws Exception {
		final boolean[] flag = new boolean[4];
		LuaFunction func = new LuaFunction() {
			@Override
			public Object[] call(Object[] args) throws LuaRuntimeException {
				int first = ((Long)args[0]).intValue();
				assertThat(first, is(args.length - 1));
				for (int i = 0; i < first; i++) {
					assertThat(((Long)args[i + 1]).intValue(), is(i));
				}
				flag[first] = true;
				return null;
			}
		};
		lua.addGlobalFunction("func", func,
			LuaArg.LONG, LuaArg.LONG_VAR_ARGS);
		lua.execString(
				"func(0)\n" +
				"func(1, 0)\n" +
				"func(2, 0, 1)\n" +
				"func(3, 0, 1, 2)\n",
				"callVerArgsFunction.lua");
		assertThat(flag, is(new boolean[] { true, true, true, true }));
	}

	@Test
	public void replacePrint() throws Exception {
		final String keyword = "replace test";
		final boolean[] flag = new boolean[2];
		lua.openStdLibs();
		lua.setPrintFunction(new LuaPrint() {
			@Override
			public void writeString(String str) {
				if (keyword.equals(str)) {
					flag[0] = true;
				}
			}
			@Override
			public void writeLine() {
				if (flag[0]) {
					flag[1] = true;
				}
			}
		});
		lua.execString(String.format("print(\"%s\")", keyword),
				"replacePrint.lua");
		assertTrue(flag[0]);
		assertTrue(flag[1]);
	}

	@Test
	public void printJapanese() throws Exception {
		final String keyword = "あいうえお";
		final boolean[] flag = new boolean[2];
		lua.openStdLibs();
		lua.setPrintFunction(new LuaPrint() {
			@Override
			public void writeString(String str) {
				if (keyword.equals(str)) {
					flag[0] = true;
				}
			}
			@Override
			public void writeLine() {
				if (flag[0]) {
					flag[1] = true;
				}
			}
		});
		lua.execString(String.format("print(\"%s\")", keyword),
				"replacePrint.lua");
		assertTrue(flag[0]);
		assertTrue(flag[1]);
	}

}
