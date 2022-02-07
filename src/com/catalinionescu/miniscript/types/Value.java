package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;

/**
 * Value: abstract base class for the MiniScript type hierarchy.
 * Defines a number of handy methods that you can call on ANY value (though some of these do nothing for some types).
 *
 */
public abstract class Value {
	/// <summary>
	/// Get the current value of this Value in the given context.  Basic types
	/// evaluate to themselves, but some types (e.g. variable references) may
	/// evaluate to something else.
	/// </summary>
	/// <param name="context">TAC context to evaluate in</param>
	/// <returns>value of this value (possibly the same as this)</returns>
	public Value Val(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		return this;		// most types evaluate to themselves
	}
	
	@Override
	public String toString() {
		return toString(null);
	}
	
	public abstract String toString(Machine vm);
	
	/// <summary>
	/// This version of Val is like the one above, but also returns
	/// (via the output parameter) the ValMap the value was found in,
	/// which could be several steps up the __isa chain.
	/// </summary>
	/// <returns>The value.</returns>
	/// <param name="context">Context.</param>
	/// <param name="valueFoundIn">Value found in.</param>
	public Pair<Value, ValMap> ValPair(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// , out ValMap valueFoundIn
		return new Pair<Value, ValMap>(this, null);
	}
	
	/// <summary>
	/// Similar to Val, but recurses into the sub-values contained by this
	/// value (if it happens to be a container, such as a list or map).
	/// </summary>
	/// <param name="context">context in which to evaluate</param>
	/// <returns>fully-evaluated value</returns>
	public Value FullEval(Context context)  throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		return this;
	}
	
	/// <summary>
	/// Get the numeric value of this Value as an integer.
	/// </summary>
	/// <returns>this value, as signed integer</returns>
	public int IntValue() {
		return (int)DoubleValue();
	}
	
	/// <summary>
	/// Get the numeric value of this Value as an unsigned integer.
	/// </summary>
	/// <returns>this value, as unsigned int</returns>
	public int UIntValue() {
		return (int)DoubleValue();
	}
	
	/// <summary>
	/// Get the numeric value of this Value as a single-precision float.
	/// </summary>
	/// <returns>this value, as a float</returns>
	public float FloatValue() {
		return (float)DoubleValue();
	}
	
	/// <summary>
	/// Get the numeric value of this Value as a double-precision floating-point number.
	/// </summary>
	/// <returns>this value, as a double</returns>
	public double DoubleValue() {
		return 0;				// most types don't have a numeric value
	}
	
	/// <summary>
	/// Get the boolean (truth) value of this Value.  By default, we consider
	/// any numeric value other than zero to be true.  (But subclasses override
	/// this with different criteria for strings, lists, and maps.)
	/// </summary>
	/// <returns>this value, as a bool</returns>
	public boolean BoolValue() {
		return IntValue() != 0;
	}
	
	public String CodeForm(Machine vm) {
		return CodeForm(vm, -1);
	}
	
	/// <summary>
	/// Get this value in the form of a MiniScript literal.
	/// </summary>
	/// <param name="recursionLimit">how deeply we can recurse, or -1 for no limit</param>
	/// <returns></returns>
	public String CodeForm(Machine vm, int recursionLimit) {
		return toString(vm);
	}
	
	public int Hash() {
		return Hash(16);
	}
	
	/// <summary>
	/// Get a hash value for this Value.  Two values that are considered
	/// equal will return the same hash value.
	/// </summary>
	/// <returns>hash value</returns>
	public abstract int Hash(int recursionDepth);
	
	public double Equality(Value rhs) {
		return Equality(rhs, 16);
	}
	
	/// <summary>
	/// Check whether this Value is equal to another Value.
	/// </summary>
	/// <param name="rhs">other value to compare to</param>
	/// <returns>1if these values are considered equal; 0 if not equal; 0.5 if unsure</returns>
	public abstract double Equality(Value rhs, int recursionDepth);
	
	/// <summary>
	/// Can we set elements within this value?  (I.e., is it a list or map?)
	/// </summary>
	/// <returns>true if SetElem can work; false if it does nothing</returns>
	public boolean CanSetElem() { return false; }
	
	/// <summary>
	/// Set an element associated with the given index within this Value.
	/// </summary>
	/// <param name="index">index/key for the value to set</param>
	/// <param name="value">value to set</param>
	public void SetElem(Value index, Value value)  throws IndexException {}

	/// <summary>
	/// Return whether this value is the given type (or some subclass thereof)
	/// in the context of the given virtual machine.
	/// </summary>
	public boolean IsA(Value type, Machine vm) {
		return false;
	}

	/// <summary>
	/// Compare two Values for sorting purposes.
	/// </summary>
	public static int Compare(Value x, Value y) {
		// Always sort null to the end of the list.
		if (x == null) {
			if (y == null) return 0;
			return 1;
		}
		if (y == null) return -1;
		// If either argument is a string, do a string comparison
		if (x instanceof ValString || y instanceof ValString) {
			String sx = x.toString();
			String sy = y.toString();
			return sx.compareTo(sy);
		}
		
		// If both arguments are numbers, compare numerically
		if (x instanceof ValNumber && y instanceof ValNumber) {
			double fx = ((ValNumber) x).value;
			double fy = ((ValNumber) y).value;
			if (fx < fy) return -1;
			if (fx > fy) return 1;
			return 0;
		}
		// Otherwise, consider all values equal, for sorting purposes.
		return 0;
	}
}
