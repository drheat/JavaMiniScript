package com.catalinionescu.miniscript.exceptions;

import com.catalinionescu.miniscript.types.Value;

public class Check {
	public static void Range(int i, int min, int max) throws IndexException {
		Range(i, min, max, "index");
	}
	
	public static void Range(int i, int min, int max, String desc) throws IndexException {
		if (i < min || i > max) {
			throw new IndexException(String.format("Index Error: %s (%d) out of range (%d to %d)", desc, i, min, max));
		}
	}
	
	public static void Type(Value val, Class<?> requiredType) throws TypeException {
		Type(val, requiredType, null);
	}
	
	public static void Type(Value val, Class<?> requiredType, String desc) throws TypeException {
		if (!(val.getClass().equals(requiredType))) {
			String typeStr = val == null ? "null" : "a " + val.getClass().getCanonicalName();
			throw new TypeException(String.format("got %s where a %s was required%s", typeStr, requiredType, desc == null ? null : " (" + desc + ")"));
		}
	}
}
