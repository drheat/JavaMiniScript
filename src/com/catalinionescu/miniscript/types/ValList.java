package com.catalinionescu.miniscript.types;

import java.util.ArrayList;
import java.util.List;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;

/// <summary>
/// ValList represents a MiniScript list (which, under the hood, is
/// just a wrapper for a List of Values).
/// </summary>
public class ValList extends Value {
	public static long maxSize = 0xFFFFFF;		// about 16 MB
	
	public List<Value> values;
	
	public ValList() {
		this(null);
	}

	public ValList(List<Value> values) {
		this.values = values == null ? new ArrayList<Value>() : values;
	}

	@Override
	public Value FullEval(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// Evaluate each of our list elements, and if any of those is
		// a variable or temp, then resolve those now.
		// CAUTION: do not mutate our original list!  We may need
		// it in its original form on future iterations.
		ValList result = null;
		for (int i = 0; i < values.size(); i++) {
			boolean copied = false;
			if (values.get(i) instanceof ValTemp || values.get(i) instanceof ValVar) {
				Value newVal = values.get(i).Val(context);
				if (newVal != values.get(i)) {
					// OK, something changed, so we're going to need a new copy of the list.
					if (result == null) {
						result = new ValList();
						for (int j = 0; j < i; j++) result.values.add(values.get(j));
					}
					result.values.add(newVal);
					copied = true;
				}
			}
			if (!copied && result != null) {
				// No change; but we have new results to return, so copy it as-is
				result.values.add(values.get(i));
			}
		}
		return result == null ? this : result;
	}

	public ValList EvalCopy(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// Create a copy of this list, evaluating its members as we go.
		// This is used when a list literal appears in the source, to
		// ensure that each time that code executes, we get a new, distinct
		// mutable object, rather than the same object multiple times.
		ValList result = new ValList();
		for (int i = 0; i < values.size(); i++) {
			result.values.add(values.get(i) == null ? null : values.get(i).Val(context));
		}
		return result;
	}

	@Override
	public String CodeForm(Machine vm, int recursionLimit) {
		if (recursionLimit == 0) return "[...]";
		if (recursionLimit > 0 && recursionLimit < 3 && vm != null) {
			String shortName = vm.FindShortName(this);
			if (shortName != null) return shortName;
		}
		String[] strs = new String[values.size()];
		for (int i = 0; i < values.size(); i++) {
			if (values.get(i) == null) strs[i] = "null";
			else strs[i] = values.get(i).CodeForm(vm, recursionLimit - 1);
		}
		return "[" + String.join(", ", strs) + "]";
	}

	@Override
	public String ToString(Machine vm) {
		return CodeForm(vm, 3);
	}

	@Override
	public boolean BoolValue() {
		// A list is considered true if it is nonempty.
		return values != null && values.size() > 0;
	}

	@Override
	public boolean IsA(Value type, Machine vm) {
		return type == vm.listType;
	}

	@Override
	public int Hash(int recursionDepth) {
		//return values.GetHashCode();
		int result = values.size();
		if (recursionDepth < 1) return result;
		for (int i = 0; i < values.size(); i++) {
			result ^= values.get(i).Hash(recursionDepth-1);
		}
		return result;
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		if (!(rhs instanceof ValList)) return 0;
		List<Value> rhl = ((ValList)rhs).values;
		if (rhl == values) return 1;  // (same list)
		int count = values.size();
		if (count != rhl.size()) return 0;
		if (recursionDepth < 1) return 0.5;		// in too deep
		double result = 1;
		for (int i = 0; i < count; i++) {
			result *= values.get(i).Equality(rhl.get(i), recursionDepth-1);
			if (result <= 0) break;
		}
		return result;
	}

	@Override
	public boolean CanSetElem() { return true; }

	@Override
	public void SetElem(Value index, Value value) throws IndexException {
		int i = index.IntValue();
		if (i < 0) i += values.size();
		if (i < 0 || i >= values.size()) {
			throw new IndexException("Index Error (list index " + index + " out of range)");
		}
		values.set(i, value);
	}

	public Value GetElem(Value index) throws IndexException {
		int i = index.IntValue();
		if (i < 0) i += values.size();
		if (i < 0 || i >= values.size()) {
			throw new IndexException("Index Error (list index " + index + " out of range)");

		}
		return values.get(i);
	}
}
