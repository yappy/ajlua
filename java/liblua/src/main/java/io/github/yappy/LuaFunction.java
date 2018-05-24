package io.github.yappy;

public interface LuaFunction {

	/**
	 * Invoked when called from Lua.
	 *
	 * <table border="1">
	 * <caption>Lua to Java parameters</caption>
	 * <tr><th>Lua</th><th>Java</th></tr>
	 * <tr><td>nil</td><td>null</td></tr>
	 * <tr><td>boolean</td><td>Boolean</td></tr>
	 * <tr><td>number</td><td>Double</td></tr>
	 * <tr><td>string</td><td>String</td></tr>
	 * </table>
	 *
	 * <table border="1">
	 * <caption>Java to Lua results</caption>
	 * <tr><th>Java</th><th>Lua</th></tr>
	 * <tr><td>null</td><td>nil</td></tr>
	 * <tr><td>Boolean</td><td>boolean</td></tr>
	 * <tr><td>Number(Byte, Double, Float, Integer, Long, Short, etc.)</td><td>number</td></tr>
	 * <tr><td>String</td><td>string</td></tr>
	 * </table>
	 *
	 * @param params Function parameters.
	 * @return Function results. null means 0-length results.
	 * @throws Exception Its message will be converted to lua error.
	 */
	Object[] call(Object[] params) throws Exception;

}