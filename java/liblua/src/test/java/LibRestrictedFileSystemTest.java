import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

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
		lua.openStdLibs();
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

	private static void prepairFile(File file, String content) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file), StandardCharsets.UTF_8))){
			writer.write(content);
		}
	}


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

	@Test
	public void readline() throws Exception {
		File testFile = new File(DIR, "readline.txt");
		prepairFile(testFile, "hello\ntakenoko\n");
		lua.execString(
				"local fd = fs.open(\"readline.txt\", \"r\")" +
				"local s1 = fs.readline(fd)\n" +
				"local s2 = fs.readline(fd)\n" +
				"local s3 = fs.readline(fd)\n" +
				"assert(s1 == \"hello\", s1)\n" +
				"assert(s2 == \"takenoko\", s2)\n" +
				"assert(s3 == nil, s3)\n",
				"readline.lua");
	}

}
