package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Machine;

/// <summary>
/// ValNumber represents a numeric (double-precision floating point) value in MiniScript.
/// Since we also use numbers to represent boolean values, ValNumber does that job too.
/// </summary>
public class ValNumber extends Value {
	public double value;

	public ValNumber(double value) {
		this.value = value;
	}

	@Override
	public String ToString(Machine vm) {
		// TODO: Review this for compliance with standard C# Miniscript
		// Convert to a string in the standard MiniScript way.
		if (value % 1.0 == 0.0) {
			// integer values as integers
			return String.format("%d", (int) value);	// value.ToString("0");
		} else if (value > 1E10 || value < -1E10 || (value < 1E-6 && value > -1E-6)) {
			// very large/small numbers in exponential form
			String s = String.format("%E", value); // value.ToString("E6");
			s = s.replace("E-00", "E-0");
			return s;
		} else {
			// all others in decimal form, with 1-6 digits past the decimal point
			return String.format("%f", value); //value.ToString("0.0#####");
		}
	}

	@Override
	public int IntValue() {
		return (int)value;
	}

	@Override
	public double DoubleValue() {
		return value;
	}
	
	@Override
	public boolean BoolValue() {
		// Any nonzero value is considered true, when treated as a bool.
		return value != 0;
	}

	@Override
	public boolean IsA(Value type, Machine vm) {
		return type == vm.numberType;
	}

	@Override
	public int Hash(int recursionDepth) {
		return Double.valueOf(value).hashCode();
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		return rhs instanceof ValNumber && ((ValNumber)rhs).value == value ? 1 : 0;
	}

	/// <summary>
	/// Handy accessor to a shared "zero" (0) value.
	/// IMPORTANT: do not alter the value of the object returned!
	/// </summary>
	public final static ValNumber zero = new ValNumber(0);
	/// <summary>
	/// Handy accessor to a shared "one" (1) value.
	/// IMPORTANT: do not alter the value of the object returned!
	/// </summary>
	public final static ValNumber one = new ValNumber(1);
	
	/// <summary>
	/// Convenience method to get a reference to zero or one, according
	/// to the given boolean.  (Note that this only covers Boolean
	/// truth values; MiniScript also allows fuzzy truth values, like
	/// 0.483, but obviously this method won't help with that.)
	/// IMPORTANT: do not alter the value of the object returned!
	/// </summary>
	/// <param name="truthValue">whether to return 1 (true) or 0 (false)</param>
	/// <returns>ValNumber.one or ValNumber.zero</returns>
	public static ValNumber Truth(boolean truthValue) {
		return truthValue ? one : zero;
	}
	
	/// <summary>
	/// Basically this just makes a ValNumber out of a double,
	/// BUT it is optimized for the case where the given value
	///	is either 0 or 1 (as is usually the case with truth tests).
	/// </summary>
	public static ValNumber Truth(double truthValue) {
		if (truthValue == 0.0) return zero;
		if (truthValue == 1.0) return one;
		return new ValNumber(truthValue);
	}
}
