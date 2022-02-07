package com.catalinionescu.miniscript.utils;

public class StringUtil {
	public static String removeFirst(String inputString, String stringToRemove) {
		if (inputString == null || inputString.isEmpty()) {
			return "";
		}

		if (stringToRemove == null || stringToRemove.isEmpty()) {
			return inputString;
		}

		int length = stringToRemove.length();
		int inputLength = inputString.length();

		int startingIndexofTheStringToReplace = inputString.indexOf(stringToRemove);

		return inputString.substring(0, startingIndexofTheStringToReplace) + inputString.substring(startingIndexofTheStringToReplace + length, inputLength);
	}
	
	public static String replaceFirst(String inputString, String stringToReplace, String stringToReplaceWith) {
		if (inputString == null || inputString.isEmpty()) {
			return "";
		}

		if (stringToReplace == null || stringToReplace.isEmpty() || stringToReplaceWith == null || stringToReplaceWith.isEmpty()) {
			return inputString;
		}

		int length = stringToReplace.length();
		int inputLength = inputString.length();

		int startingIndexofTheStringToReplace = inputString.indexOf(stringToReplace);

		return inputString.substring(0, startingIndexofTheStringToReplace) + stringToReplaceWith + inputString.substring(startingIndexofTheStringToReplace + length, inputLength);
	}
}
