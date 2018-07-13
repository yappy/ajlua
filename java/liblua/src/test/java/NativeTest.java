import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

import io.github.yappy.lua.LuaEngine;

public class NativeTest {

	private static int call(String name) throws Exception {
		Class<LuaEngine> cls = LuaEngine.class;
		Method m = cls.getDeclaredMethod(name);
		m.setAccessible(true);
		// call static name()
		return ((Integer)m.invoke(null)).intValue();
	}

	@Test
	public void time() throws Exception {
		assertEquals(0, call("testDestructor"));
	}

}
