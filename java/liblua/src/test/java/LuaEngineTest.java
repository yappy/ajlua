import io.github.yappy.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LuaEngineTest {

	@Test public void newAndClose() {
		try (LuaEngine lua = new LuaEngine()) {
			// do nothing
		}
	}



}
