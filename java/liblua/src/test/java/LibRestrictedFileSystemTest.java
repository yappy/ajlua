import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.github.yappy.lua.LuaEngine;
import io.github.yappy.lua.LuaRuntimeException;
import io.github.yappy.lua.lib.RestrictedFileSystem;

public class LibRestrictedFileSystemTest {

	private static final File DIR = new File("./testdir");
	private LuaEngine lua;

	@Before
	public void init() throws Exception {
		lua = new LuaEngine();
		if (!DIR.mkdir()) {
			throw new IOException("Cannot create test dir");
		}
		lua.addLibrary(new RestrictedFileSystem(DIR));
	}

	@After
	public void term() throws Exception {
		lua.close();
		lua = null;
		for (File file : DIR.listFiles()) {
			file.delete();
		}
		DIR.delete();
	}

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Test
	public void openClose() throws Exception {
		lua.execString(
				"local fd = fs.open(\"test.txt\", \"w\")\n" +
				"fs.close(fd)\n",
				"open.lua");
	}

	@Test
	public void openError() throws Exception {
		exception.expect(LuaRuntimeException.class);
		exception.expectMessage("Invalid file name character");
		lua.execString("fs.open(\"@badfile\")", "open.lua");
	}

}
