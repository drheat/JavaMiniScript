package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;

/// <summary>
/// ValNull is an object to represent null in places where we can't use
/// an actual null (such as a dictionary key or value).
/// </summary>
public class ValNull extends Value {
	private ValNull() {}
	
	@Override
	public String toString(Machine machine) {
		return "null";
	}
	
	@Override
	public boolean IsA(Value type, Machine vm) {
		return false;
	}

	@Override
	public int Hash(int recursionDepth) {
		return -1;
	}

	@Override
	public Value Val(Context context) {
		return null;
	}

	@Override
	public Pair<Value, ValMap> ValPair(Context context) {
		return new Pair<Value, ValMap>(this, null);
	}
	
	@Override
	public Value FullEval(Context context) {
		return null;
	}
	
	@Override
	public int IntValue() {
		return 0;
	}

	@Override
	public double DoubleValue() {
		return 0.0;
	}
	
	@Override
	public boolean BoolValue() {
		return false;
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		return (rhs == null || rhs instanceof ValNull ? 1 : 0);
	}

	
	/// <summary>
	/// Handy accessor to a shared "instance".
	/// </summary>
	public static final ValNull instance = new ValNull();
}
