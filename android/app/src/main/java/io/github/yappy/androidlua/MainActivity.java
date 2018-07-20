package io.github.yappy.androidlua;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import io.github.yappy.lua.LuaEngine;
import io.github.yappy.lua.LuaException;
import io.github.yappy.lua.LuaRuntimeException;
import io.github.yappy.lua.LuaPrint;

public class MainActivity extends AppCompatActivity {

	private static final String SAMPLE =
		"print(\"hello!\")\n" +
		"print(Build.MODEL)";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setTitle(getTitle() + ": " + LuaEngine.getLibraryVersion());

		Button button = findViewById(R.id.button3);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				runScript();
			}
		});

		EditText editText = findViewById(R.id.editText3);
		editText.setText(SAMPLE);
	}

	private void runScript() {
		EditText editText = findViewById(R.id.editText3);
		String src = editText.getText().toString();
		try (LuaEngine lua = new LuaEngine()) {
			lua.openStdLibs();
			lua.setPrintFunction(new LuaPrint() {
				@Override
				public void writeString(String str) {
					Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
				}
				@Override
				public void writeLine() {

				}
			});
			lua.addLibTable("Build");
			lua.addLibVariable("Build", "MODEL", Build.MODEL);

			lua.execString(src, "test.lua");
		}
		catch (LuaRuntimeException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		catch(LuaException e){
			Toast.makeText(this, "Error!", Toast.LENGTH_LONG).show();
		}
	}

}
