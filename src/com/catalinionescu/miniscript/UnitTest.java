package com.catalinionescu.miniscript;

import com.catalinionescu.miniscript.exceptions.LexerException;

public class UnitTest {
	public static void ReportError(String err) {
		// Set a breakpoint here if you want to drop into the debugger
		// on any unit test failure.
		System.err.println(err);
	}

	public static void ErrorIf(boolean condition, String err) {
		if (condition) ReportError(err);
	}

	public static void ErrorIfNull(Object obj) {
		if (obj == null) ReportError("Unexpected null");
	}

	public static void ErrorIfNotNull(Object obj) { 
		if (obj != null) ReportError("Expected null, but got non-null");
	}
	
	public static void ErrorIfNotEqual(String actual, String expected) {
		ErrorIfNotEqual(actual, expected, "Expected %s, got %s");
	}

	public static void ErrorIfNotEqual(String actual, String expected, String desc) {
		if (actual == expected)
			return;
		
		ReportError(String.format(desc, expected, actual));
	}
	
	public static void ErrorIfNotEqual(float actual, float expected) {
		ErrorIfNotEqual(actual, expected, "Expected %f, got %f");
	}

	public static void ErrorIfNotEqual(float actual, float expected, String desc) {
		if (actual == expected)
			return;
		
		ReportError(String.format(desc, expected, actual));
	}

	public static void Run() throws LexerException {
		Lexer.RunUnitTests();
		Parser.RunUnitTests();
	}
}
