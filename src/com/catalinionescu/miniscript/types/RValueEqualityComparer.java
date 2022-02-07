package com.catalinionescu.miniscript.types;

import java.util.Comparator;

public class RValueEqualityComparer implements Comparator<Value> {
	@Override
	public int compare(Value o1, Value o2) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean Equals(Value val1, Value val2) {
		return val1.Equality(val2) > 0;
	}

	public int GetHashCode(Value val) {
		return val.Hash();
	}

	static RValueEqualityComparer _instance = null;
	
	public static RValueEqualityComparer instance() {
		if (_instance == null) _instance = new RValueEqualityComparer();
		return _instance;
	}
}
