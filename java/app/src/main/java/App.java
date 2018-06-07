import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import io.github.yappy.LuaEngine;
import io.github.yappy.LuaException;
import io.github.yappy.LuaPrint;

public class App extends JFrame {

	private static final long serialVersionUID = 1L;

	private JTextArea srcArea;
	private JTextArea outArea;
	JSplitPane split;

	public App() {
		super("Test App");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationByPlatform(true);
		setSize(640, 480);

		srcArea = new JTextArea("print(\"hello!\")\n");
		outArea = new JTextArea();
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, srcArea, outArea);
		split.setDividerLocation(Integer.MAX_VALUE);
		add(split, BorderLayout.CENTER);

		JButton btn = new JButton("Run");
		btn.addActionListener((event) -> {
			clickAction();
		});
		add(btn, BorderLayout.SOUTH);
	}

	private void clickAction() {
		outArea.setText("");
		try {
			runScript(srcArea.getText(), "test.lua");
			outArea.append("[Exit Successfully]");
		} catch (LuaException e) {
			outArea.append("[Error]\n" + e.getMessage() + "\n");
		} catch (InterruptedException e) {
			outArea.append("[Interrupted]\n");
		}
		split.setDividerLocation(split.getMaximumDividerLocation());
	}

	private void runScript(String src, String chunkName) throws LuaException, InterruptedException {
		try (LuaEngine lua = new LuaEngine()) {
			lua.openStdLibs();
			lua.setPrintFunction(new LuaPrint() {
				@Override
				public void writeString(String str) {
					outArea.append(str);
				}

				@Override
				public void writeLine() {
					outArea.append("\n");
				}
			});

			lua.addGlobalFunction("jget", (params) -> {
				return new Object[] { 3.14, 2 };
			});

			lua.execString(src, chunkName);
		}
	}

	public static void main(String[] args) throws Exception {
		App app = new App();
		app.setVisible(true);
	}

}
