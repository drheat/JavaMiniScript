package com.catalinionescu.miniscript.types;

import java.util.Comparator;

public class ValueSorter implements Comparator<Value> {
	public static ValueSorter instance = new ValueSorter();

	@Override
	public int compare(Value x, Value y) {
		return Value.Compare(x, y);
	}
}