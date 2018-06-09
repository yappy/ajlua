import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

import io.github.yappy.LuaArg;
import io.github.yappy.LuaEngine;
import io.github.yappy.LuaException;
import io.github.yappy.LuaFunction;
import io.github.yappy.LuaRuntimeException;
import io.github.yappy.LuaSyntaxException;

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
		exception.expect(InterruptedException.class);
		Thread main = Thread.currentThread();
		Thread sub = new Thread(() -> {
			try { Thread.sleep(100); } catch(Exception e) {}
			main.interrupt();
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
		boolean[] flag = new boolean[1];
		LuaFunction func = (Object[] args) -> {
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
		};
		lua.addGlobalFunction("func", func,
			LuaArg.BOOLEAN, LuaArg.LONG, LuaArg.DOUBLE, LuaArg.STRING);
		lua.execString("func(true, 7, 3.14, \"hello\")",
			"callGlobalFunction.lua");
		assertTrue(flag[0]);
	}

	@Test
	public void callLibFunction() throws Exception {
		boolean[] flag = new boolean[1];
		LuaFunction func = (Object[] args) -> {
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
		boolean[] flag = new boolean[1];
		LuaFunction func = (Object[] args) -> {
			assertThat(args[0], is(nullValue()));
			assertThat(args[1], is(nullValue()));
			assertThat(args[2], is(nullValue()));
			assertThat(args[3], is(nullValue()));
			flag[0] = true;
			return null;
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
		boolean[] flag = new boolean[1];
		LuaFunction func = (Object[] args) -> {
			assertThat(args[0], is(nullValue()));
			assertThat(args[1], is(nullValue()));
			assertThat(args[2], is(nullValue()));
			assertThat(args[3], is(nullValue()));
			flag[0] = true;
			return null;
		};
		lua.addLibTable("lib");
		lua.addLibFunction("lib", "func", func,
			LuaArg.BOOLEAN_OR_NIL, LuaArg.LONG_OR_NIL,
			LuaArg.DOUBLE_OR_NIL, LuaArg.STRING_OR_NIL);
		lua.execString("lib.func(nil, nil, nil, nil)",
			"callNullableLibFunction.lua");
		assertTrue(flag[0]);
	}

}
