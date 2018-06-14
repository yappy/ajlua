/**
 * Lua engine and related classes.
 *
 * <h1>Tutorial</h1>
 *
 * <h2>Initialize and Finalize Lua Engine</h2>
 * <p>
 * Call {@link io.github.yappy.lua.LuaEngine#LuaEngine()} constructor and
 * {@link io.github.yappy.lua.LuaEngine#close()} method.
 * </p>
 * <pre>{@code
 * LuaEngine lua = new LuaEngine();
 * lua.close();
 * }</pre>
 *
 * <p>The try-with-resources statement is useful to ensure closing.</p>
 * <pre>{@code
 * try (LuaEngine lua = new LuaEngine()) {
 *     ...
 * }
 * }</pre>
 *
 * <h2>Open Lua Standard Library</h2>
 * <p>
 * Call {@link io.github.yappy.lua.LuaEngine#openStdLibs()}.
 * With this no parameters version, default set will be loaded.
 * You can use {@link io.github.yappy.lua.LuaEngine#openStdLibs(java.util.Set)} 
 * to choose libraries to be loaded.
 * </p>
 * <p>
 * See also for Lua standard library:
 * <a href="https://www.lua.org/manual/5.3/manual.html#6">https://www.lua.org/manual/5.3/manual.html#6</a>
 * </p>
 * <pre>{@code
 * try (LuaEngine lua = new LuaEngine()) {
 *     lua.openStdLibs();
 *     ...
 * }
 * }</pre>
 *
 * <h2>Set print function callback action</h2>
 * <p>
 * Call {@link io.github.yappy.lua.LuaEngine#setPrintFunction(LuaPrint)}.
 * LuaEngine replaces Lua standard <i>print</i> function automatically at
 * {@link io.github.yappy.lua.LuaEngine#openStdLibs()}.</p>
 * <pre><code>
 * lua.setPrintFunction(new LuaPrint() {
 *     &#064;Override
 *     public void writeString(String str) {
 *         System.err.println(str);
 *     }
 *     &#064;Override
 *     public void writeLine() {
 *         System.err.println();
 *     }
 * });
 * </code></pre>
 *
 * <h2>Execute a string as Lua source code</h2>
 * <p>
 * Call {@link io.github.yappy.lua.LuaEngine#execString(String, String)}.
 * The first string is source code to be executed.
 * The second string is chunk name. It will be used for error message, etc.
 * </p>
 * <pre>{@code
 * lua.execString("print(\"hello, world\")", "hello.lua");
 * }</pre>
 *
 * @see io.github.yappy.lua.LuaEngine
 * @see io.github.yappy.lua.LuaStdLib
 * @see io.github.yappy.lua.LuaPrint
 * @author yappy
 */
package io.github.yappy.lua;
