package io.github.yappy;

/**
 * Customized Lua standard print() function callback.
 * @author yappy
 */
public interface LuaPrint {

	/**
	 * Write string callback.
	 * @param str String to be written.
	 */
	void writeString(String str);

	/**
	 * Write new line callback.
	 */
	void writeLine();

}
