import io.github.yappy.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import static org.junit.Assert.*;

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
		exception.expect(LuaException.class);
		exception.expectMessage("execution aborted");
		long start = System.currentTimeMillis();
		lua.execString((type, line) -> {
				boolean wait = System.currentTimeMillis() - start < 100;
				return !wait;
			},
			"while true do end",
			"timeoutError.lua");
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

}
