package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;

public class ValTemp extends Value {
	public int tempNum;

	public ValTemp(int tempNum) {
		this.tempNum = tempNum;
	}

	@Override
	public Value Val(Context context) {
		return context.GetTemp(tempNum);
	}

	@Override
	public Pair<Value, ValMap> ValPair(Context context) {
		return new Pair<Value, ValMap>(context.GetTemp(tempNum), null);
	}

	@Override
	public String ToString(Machine vm) {
		return String.format("_%d", tempNum);
	}

	@Override
	public int Hash(int recursionDepth) {
		return tempNum;
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		return rhs instanceof ValTemp && ((ValTemp)rhs).tempNum == tempNum ? 1 : 0;
	}
}
