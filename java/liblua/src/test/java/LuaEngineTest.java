import io.github.yappy.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
	@Ignore
	public void memoryError() throws Exception {
		exception.expect(LuaException.class);
		exception.expectMessage("memory error");
		lua.execString(
			"t = {}\n" +
			"t[1] = \"nanglasgfsaafsdisabgksbsgabbikgbslieagbilaegbilrgag\"\n" +
			"i = 2\n" +
			"while true do\n" +
			"  t[i] = t[i - 1] .. t[i - 1]\n" +
			"  i = i + 1\n" +
			"end",
			"runtimeError.lua");
	}

}
