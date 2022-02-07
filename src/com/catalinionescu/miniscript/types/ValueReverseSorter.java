package com.catalinionescu.miniscript.types;

import java.util.Comparator;

public class ValueReverseSorter implements Comparator<Value> {
	public static ValueReverseSorter instance = new ValueReverseSorter();

	@Override
	public int compare(Value x, Value y) {
		return Value.Compare(y, x);
	}

}
