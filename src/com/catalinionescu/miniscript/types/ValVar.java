package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;

public class ValVar extends Value {
	public String identifier;
	public boolean noInvoke;	// reflects use of "@" (address-of) operator

	public ValVar(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public Value Val(Context context) throws UndefinedIdentifierException {
		if (this == self) return context.self;
		return context.GetVar(identifier);
	}

	@Override
	public Pair<Value, ValMap> ValPair(Context context) throws UndefinedIdentifierException {
		if (this == self) return new Pair<Value, ValMap>(context.self, null);
		return new Pair<Value, ValMap>(context.GetVar(identifier), null);
	}

	@Override
	public String toString(Machine vm) {
		return noInvoke ? "@" : "" + identifier;
	}

	@Override
	public int Hash(int recursionDepth) {
		return identifier.hashCode();
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		return rhs instanceof ValVar && ((ValVar)rhs).identifier == identifier ? 1 : 0;
	}

	// Special name for the implicit result variable we assign to on expression statements:
	public static ValVar implicitResult = new ValVar("_");

	// Special var for 'self'
	public static ValVar self = new ValVar("self");
}
