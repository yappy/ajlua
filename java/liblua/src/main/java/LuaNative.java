
public class LuaNative {

	static {
		System.loadLibrary("jlua");
	}

	public static native int testMethod();

}
