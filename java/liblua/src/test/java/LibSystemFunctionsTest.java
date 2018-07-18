import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import io.github.yappy.lua.LuaAbortException;
import io.github.yappy.lua.LuaEngine;
import io.github.yappy.lua.lib.SystemFunctions;

public class LibSystemFunctionsTest {

	private LuaEngine lua;

	@Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

	@Before
	public void init() throws Exception {
		lua = new LuaEngine();
		lua.openStdLibs();
		lua.addLibrary(new SystemFunctions());
	}

	@After
	public void term() throws Exception {
		lua.close();
		lua = null;
	}

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Test
	public void time() throws Exception {
		// 10ms
		final long EPS = 10;

		long time1 = System.currentTimeMillis();
		lua.execString(
				"t = sys.time()\n",
				"time.lua");
		long time2 = ((Double)lua.getGlobalVariable("t")).longValue();
		assertTrue(time2 - time1 < EPS);
	}

	@Test
	public void sleep() throws Exception {
		// 100ms
		final long SLEEP = 100;
		// 10ms
		final long EPS = 10;

		long time1 = System.currentTimeMillis();
		lua.execString(
				String.format("t = sys.sleep(%s)\n", SLEEP),
				"sleep.lua");
		long time2 = System.currentTimeMillis();
		assertTrue(Math.abs((time2 - time1) - SLEEP) < EPS);
	}

	@Test
	public void sleepInterrupt() throws Exception {
		// 1000ms, 100ms
		final long SLEEP = 1000;
		final long INT = 100;
		// 10ms
		final long EPS = 10;

		final Thread main = Thread.currentThread();
		Thread sub = new Thread(new Runnable() {
			@Override
			public void run() {
				try { Thread.sleep(INT); } catch (Exception e) {}
				main.interrupt();
			}
		});

		sub.start();
		long time1 = System.currentTimeMillis();
		try {
			lua.execString(
					String.format("t = sys.sleep(%s)\n", SLEEP),
					"sleep.lua");
		} catch (LuaAbortException e) {
			assertTrue(e.getCause() instanceof InterruptedException);
		}
		long time2 = System.currentTimeMillis();
		sub.join();

		assertTrue(Math.abs((time2 - time1) - INT) < EPS);
	}

}
