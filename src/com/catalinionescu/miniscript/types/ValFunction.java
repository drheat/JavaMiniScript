package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Machine;

/// <summary>
/// ValFunction: a Value that is, in fact, a Function.
/// </summary>
public class ValFunction extends Value {
	public Function function;
	public ValMap outerVars;	// local variables where the function was defined (usually, the module)

	public ValFunction(Function function) {
		this.function = function;
	}
	public ValFunction(Function function, ValMap outerVars) {
		this.function = function;
        this.outerVars = outerVars;
	}

	@Override
	public String toString(Machine vm) {
		return function.toString(vm);
	}

	@Override
	public boolean BoolValue() {
		// A function value is ALWAYS considered true.
		return true;
	}

	@Override
	public boolean IsA(Value type, Machine vm) {
		return type == vm.functionType;
	}

	@Override
	public int Hash(int recursionDepth) {
		return function.hashCode();
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		// Two Function values are equal only if they refer to the exact same function
		if (!(rhs instanceof ValFunction)) return 0;
		ValFunction other = (ValFunction)rhs;
		return function == other.function ? 1 : 0;
	}

    public ValFunction BindAndCopy(ValMap contextVariables) {
        return new ValFunction(function, contextVariables);
    }
}
