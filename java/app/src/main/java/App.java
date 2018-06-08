import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import io.github.yappy.LuaEngine;
import io.github.yappy.LuaException;
import io.github.yappy.LuaPrint;

public class App extends JFrame {

	private static final long serialVersionUID = 1L;

	private ExecutorService executor = null;
	private Future<?> future = null;

	private JTextArea srcArea;
	private JTextArea outArea;
	JSplitPane split;
	private JButton button;

	public App() {
		super("Test App");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		executor = Executors.newSingleThreadExecutor();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				executor.shutdown();
			}
		});

		setLocationByPlatform(true);
		setSize(640, 480);

		srcArea = new JTextArea("print(\"hello!\")\n");
		outArea = new JTextArea();
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
				new JScrollPane(srcArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
				new JScrollPane(outArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		split.setResizeWeight(0.5);
		add(split, BorderLayout.CENTER);

		button = new JButton("Run");
		button.addActionListener((event) -> {
			onClick();
		});
		add(button, BorderLayout.SOUTH);
	}

	// on click (event thread)
	private void onClick() {
		if (future != null && !future.isDone()) {
			// interrupt
			future.cancel(true);
			return;
		}

		outArea.setText("");
		button.setText("Cancel");
		future = executor.submit(new RunTask(srcArea.getText(), "test.lua"));
	}

	// on run script finish (event thread)
	private void onFinish() {
		if (!future.isCancelled()) {
			try {
				future.get();
				outArea.append("[Exit Successfully]\n");
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof Error) {
					throw new Error(cause);
				} else if (cause instanceof RuntimeException) {
					throw new RuntimeException(cause);
				} else if (cause instanceof LuaException) {
					outArea.append("[Error]\n" + cause.getMessage() + "\n");
				} else {
					throw new RuntimeException(cause);
				}
			} catch (InterruptedException e) {
				outArea.append("[Interrupted]\n");
			}
		} else {
			outArea.append("[Cancel]\n");
		}
		button.setText("Run");
	}

	private class RunTask implements Callable<Object> {
		private String src;
		private String chunkName;

		public RunTask(String src, String chunkName) {
			this.src = src;
			this.chunkName = chunkName;
		}

		@Override
		public Object call() throws Exception {
			try (LuaEngine lua = new LuaEngine()) {
				lua.openStdLibs();
				lua.setPrintFunction(new LuaPrint() {
					@Override
					public void writeString(String str) {
						SwingUtilities.invokeLater(() -> {
							outArea.append(str);
						});
					}

					@Override
					public void writeLine() {
						SwingUtilities.invokeLater(() -> {
							outArea.append("\n");
						});
					}
				});

				lua.setGlobalFunction("jget", (params) -> {
					return new Object[] { 3.14, 2 };
				});

				lua.execString(src, chunkName);
			} finally {
				SwingUtilities.invokeLater(() -> {
					onFinish();
				});
			}
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		App app = new App();
		app.setVisible(true);
	}

}
