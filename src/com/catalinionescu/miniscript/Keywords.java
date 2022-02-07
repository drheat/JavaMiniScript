package com.catalinionescu.miniscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Keywords {
	public final static Collection<String> ALL = new ArrayList<>(Arrays.asList(
		"break",
		"continue",
		"else",
		"end",
		"for",
		"function",
		"if",
		"in",
		"isa",
		"new",
		"null",
		"then",
		"repeat",
		"return",
		"while",
		"and",
		"or",
		"not",
		"true",
		"false"));

	public static boolean isKeyword(String text) {
		return ALL.contains(text);
	}
}
