package io.github.yappy;

public interface Function {

	// @returns Results count on the stack.
	// @throws Exception Its message will be converted to lua error.
	int call() throws Exception;
}
