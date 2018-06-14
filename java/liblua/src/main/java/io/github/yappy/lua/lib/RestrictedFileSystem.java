package io.github.yappy.lua.lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.yappy.lua.LuaArg;
import io.github.yappy.lua.LuaException;
import io.github.yappy.lua.LuaFunction;
import io.github.yappy.lua.LuaRuntimeException;

@LuaLibraryTable("fs")
/**
 * <ul>
 * <li>Cannot use directories.</li>
 * <li>File name character must be [a-z][A-Z][0-9] or '.' or '_' or '-' .</li>
 * <li>File name the first character must be [a-z][A-Z][0-9]</li>
 * </ul>
 * @author yappy
 */
public class RestrictedFileSystem implements LuaLibrary {

	public static final int FILE_NAME_MAX = 16;
	public static final String FILE_NAME_CHARS_FIRST =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
			"0123456789";
	public static final String FILE_NAME_CHARS_FOLLOW =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
			"0123456789._-";

	private static final char[] CHARS_FIRST, CHARS_FOLLOW;
	private File dir;
	private Random fdGen;
	private Map<Integer, AutoCloseable> fdMap;

	static {
		char[] cs;

		cs = FILE_NAME_CHARS_FIRST.toCharArray();
		Arrays.sort(cs);
		CHARS_FIRST = cs;

		cs = FILE_NAME_CHARS_FOLLOW.toCharArray();
		Arrays.sort(cs);
		CHARS_FOLLOW = cs;
	}

	public RestrictedFileSystem(File dir) {
		this.dir = dir;
		this.fdGen = new Random(System.nanoTime());
		this.fdMap = new HashMap<>();
	}

	@Override
	public void close() throws Exception {
		for (AutoCloseable io : fdMap.values()) {
			io.close();
		}
		fdMap.clear();
	}

	private int generateFd() {
		int fd;
		do {
			fd = fdGen.nextInt();
		} while (fd < 1 || fdMap.containsKey(fd));
		return fd;
	}

	private BufferedReader getReader(int fd) throws LuaRuntimeException {
		AutoCloseable io = (AutoCloseable)fdMap.get(fd);
		if (io == null) {
			throw new LuaRuntimeException("Invalid file");
		}
		if (!(io instanceof BufferedReader)) {
			throw new LuaRuntimeException("Invalid file for read");
		}
		return (BufferedReader)io;
	}

	private BufferedWriter getWriter(int fd) throws LuaRuntimeException {
		AutoCloseable io = (AutoCloseable)fdMap.get(fd);
		if (io == null) {
			throw new LuaRuntimeException("Invalid file");
		}
		if (!(io instanceof BufferedWriter)) {
			throw new LuaRuntimeException("Invalid file for write");
		}
		return (BufferedWriter)io;
	}

	private void checkFileName(String name) throws LuaRuntimeException {
		int len = name.length();
		if (len < 1 || len > FILE_NAME_MAX) {
			throw new LuaRuntimeException("Invalid file name length: " + len);
		}
		for (int i = 0; i < len; i++) {
			char c = name.charAt(i);
			char[] allow = (i == 0) ? CHARS_FIRST : CHARS_FOLLOW;
			if (Arrays.binarySearch(allow, c) < 0) {
				throw new LuaRuntimeException("Invalid file name character: " + c);
			}
		}
	}

	@LuaLibraryFunction(name = "open", args = { LuaArg.STRING, LuaArg.STRING_OR_NIL })
	public LuaFunction open() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			String name = args[0].toString();
			String mode = (args[1] != null) ? args[1].toString() : "r";

			checkFileName(name);
			File file = new File(dir, name);
			AutoCloseable io;
			try {
				switch (mode) {
				case "r":
					io = new BufferedReader(new InputStreamReader(
							new FileInputStream(file), StandardCharsets.UTF_8));
					break;
				case "w":
					io = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(file, false), StandardCharsets.UTF_8));
					break;
				case "a":
					io = new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(file, true), StandardCharsets.UTF_8));
				default:
					throw new LuaRuntimeException("Invalid mode");
				}
			} catch(FileNotFoundException e) {
				// Open failed.
				throw new LuaRuntimeException("Open failed");
			}

			int fd = generateFd();
			fdMap.put(fd, io);
			return new Object[] { fd };
		}
	};}

	@LuaLibraryFunction(name = "close", args = { LuaArg.LONG })
	public LuaFunction _close() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			int fd = ((Long)args[0]).intValue();
			AutoCloseable io = fdMap.remove(fd);
			if (io == null) {
				throw new LuaRuntimeException("Invalid file");
			}
			try {
				io.close();
			} catch (Exception e) {
				throw new LuaRuntimeException("IO error", e);
			}
			return null;
		}
	};}

	@LuaLibraryFunction(name = "readline", args = { LuaArg.LONG })
	public LuaFunction readline() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			int fd = ((Long)args[0]).intValue();
			BufferedReader reader = getReader(fd);

			try {
				// return nil if EOF
				return new Object[] { reader.readLine() };
			} catch (IOException e) {
				throw new LuaRuntimeException("IO error", e);
			}
		}
	};}

	@LuaLibraryFunction(name = "readall", args = { LuaArg.LONG })
	public LuaFunction readall() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			int fd = ((Long)args[0]).intValue();
			BufferedReader reader = getReader(fd);

			List<String> lines = new ArrayList<>();
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					lines.add(line);
				}
			} catch (IOException e) {
				throw new LuaRuntimeException("IO error", e);
			}
			return new Object[] { lines.toArray() };
		}
	};}

	@LuaLibraryFunction(name = "write", args = { LuaArg.LONG, LuaArg.STRING_VAR_ARGS })
	public LuaFunction write() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			int fd = ((Long)args[0]).intValue();
			BufferedWriter writer = getWriter(fd);

			try {
				for (int i = 1; i < args.length; i++) {
					writer.write(args[i].toString());
				}
			} catch (IOException e) {
				throw new LuaRuntimeException("IO error", e);
			}
			return null;
		}
	};}

	@LuaLibraryFunction(name = "writeline", args = { LuaArg.LONG, LuaArg.STRING_VAR_ARGS })
	public LuaFunction writeline() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			int fd = ((Long)args[0]).intValue();
			BufferedWriter writer = getWriter(fd);

			try {
				for (int i = 1; i < args.length; i++) {
					writer.write(args[i].toString());
					writer.write('\n');
				}
				// writeline(fd) to new line
				if (args.length == 1) {
					writer.write('\n');
				}
			} catch (IOException e) {
				throw new LuaRuntimeException("IO error", e);
			}
			return null;
		}
	};}

	@LuaLibraryFunction(name = "list", args = {})
	public LuaFunction list() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			String[] result = dir.list();
			if (result == null) {
				throw new LuaRuntimeException("IO error");
			}
			return new Object[] { result };
		}
	};}

	@LuaLibraryFunction(name = "exist", args = { LuaArg.STRING })
	public LuaFunction exist() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			String name = args[0].toString();

			checkFileName(name);
			File file = new File(dir, name);
			return new Object[] { file.exists() };
		}
	};}

	@LuaLibraryFunction(name = "rename", args = { LuaArg.STRING, LuaArg.STRING })
	public LuaFunction rename() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			String from = args[0].toString();
			String to = args[1].toString();

			checkFileName(from);
			checkFileName(to);
			File fileFrom = new File(dir, from);
			File fileTo = new File(dir, to);
			if (!fileFrom.renameTo(fileTo)) {
				throw new LuaRuntimeException("IO error");
			}
			return null;
		}
	};}

	@LuaLibraryFunction(name = "delete", args = { LuaArg.STRING })
	public LuaFunction delete() { return new LuaFunction() {
		@Override
		public Object[] call(Object[] args) throws LuaException {
			String name = args[0].toString();

			checkFileName(name);
			File file = new File(dir, name);
			if (!file.delete()) {
				throw new LuaRuntimeException("IO error");
			}
			return null;
		}
	};}

}
