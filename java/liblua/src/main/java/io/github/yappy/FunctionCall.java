package io.github.yappy;

// package private
interface FunctionCall {
	// @returns results count on the stack
	int call(int id) throws Exception;
}
