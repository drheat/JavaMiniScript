package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Machine;
import com.catalinionescu.miniscript.exceptions.IndexException;

/// <summary>
/// ValString represents a string (text) value.
/// </summary>
public class ValString extends Value {
	public static long maxSize = 0xFFFFFF;		// about 16M elements
	
	public String value;
	
	ValString() {
		this.value = "";
	}

	public ValString(String value) {
		this.value = (value == null || value.isEmpty()) ? empty.value : value;
	}

	@Override
	public String toString(Machine vm) {
		return value;
	}

	@Override
	public String CodeForm(Machine vm, int recursionLimit) {
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}

	@Override
	public boolean BoolValue() {
		// Any nonempty string is considered true.
		return value != null && !value.isEmpty();
	}

	@Override
	public boolean IsA(Value type, Machine vm) {
		return type == vm.stringType;
	}

	@Override
	public int Hash(int recursionDepth) {
		return value.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof ValString)) return false;
		ValString other = (ValString) o;
		return (other.value == null && value == null) || other.value.equals(value);
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		// String equality is treated the same as in C#.
		return rhs instanceof ValString && ((ValString)rhs).value.equals(value) ? 1 : 0;
	}

	public Value GetElem(Value index) throws IndexException {
		int i = index.IntValue();
		if (i < 0) i += value.length();
		if (i < 0 || i >= value.length()) {
			throw new IndexException("Index Error (string index " + index + " out of range)");

		}
		return new ValString(value.substring(i, i + 1));
	}

	// Magic identifier for the is-a entry in the class system:
	public static ValString magicIsA = new ValString("__isa");

	/// <summary>
	/// Handy accessor for an empty ValString.
	/// IMPORTANT: do not alter the value of the object returned!
	/// </summary>
	public static ValString empty = new ValString();
}
