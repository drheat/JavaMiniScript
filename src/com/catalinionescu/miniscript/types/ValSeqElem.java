package com.catalinionescu.miniscript.types;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.intrinsics.Intrinsics;

public class ValSeqElem extends Value {
	public Value sequence;
	public Value index;
	public boolean noInvoke;	// reflects use of "@" (address-of) operator

	public ValSeqElem(Value sequence, Value index) {
		this.sequence = sequence;
		this.index = index;
	}

	/// <summary>
	/// Look up the given identifier in the given sequence, walking the type chain
	/// until we either find it, or fail.
	/// </summary>
	/// <param name="sequence">Sequence (object) to look in.</param>
	/// <param name="identifier">Identifier to look for.</param>
	/// <param name="context">Context.</param>
	public static Pair<Value, ValMap> Resolve(Value sequence, String identifier, Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// , out ValMap valueFoundIn
		boolean includeMapType = true;
		int loopsLeft = 1000;		// (max __isa chain depth)
		while (sequence != null) {
			if (sequence instanceof ValTemp || sequence instanceof ValVar) sequence = sequence.Val(context);
			if (sequence instanceof ValMap) {
				// If the map contains this identifier, return its value.
				TempValString idVal = TempValString.Get(identifier);
				if (((ValMap)sequence).map.containsKey(idVal)) {
					Value result = ((ValMap)sequence).map.get(idVal);
					TempValString.Release(idVal);
					
					return new Pair<Value, ValMap>(result, (ValMap) sequence);
				}
				TempValString.Release(idVal);
				
				// Otherwise, if we have an __isa, try that next.
				if (loopsLeft < 0) return null;		// (unless we've hit the loop limit)
				if (!((ValMap)sequence).map.containsKey(ValString.magicIsA)) {
					// ...and if we don't have an __isa, try the generic map type if allowed
					if (!includeMapType) throw new KeyException(identifier);
					sequence = context.vm.mapType == null ? Intrinsics.MapType() : context.vm.mapType;
					includeMapType = false;
				} else {
					sequence = ((ValMap)sequence).map.get(ValString.magicIsA);
				}
			} else if (sequence instanceof ValList) {
				sequence = context.vm.listType == null ? Intrinsics.ListType() : context.vm.listType;
				includeMapType = false;
			} else if (sequence instanceof ValString) {
				sequence = context.vm.stringType == null ? Intrinsics.StringType() : context.vm.stringType;
				includeMapType = false;
			} else if (sequence instanceof ValNumber) {
				sequence = context.vm.numberType == null ? Intrinsics.NumberType() : context.vm.numberType;
				includeMapType = false;
			} else if (sequence instanceof ValFunction) {
				sequence = context.vm.functionType == null ? Intrinsics.FunctionType() : context.vm.functionType;
				includeMapType = false;
			} else {
				throw new TypeException("Type Error (while attempting to look up " + identifier + ")");
			}
			loopsLeft--;
		}
		return null;
	}

	@Override
	public Value Val(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		return ValPair(context).getFirst();
	}
	
	@Override
	public Pair<Value, ValMap> ValPair(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		Value baseSeq = sequence;
		if (sequence == ValVar.self) {
			baseSeq = context.self;
		}
		
		Value idxVal = index == null ? null : index.Val(context);
		if (idxVal instanceof ValString) return Resolve(baseSeq, ((ValString)idxVal).value, context);
		// Ok, we're searching for something that's not a string;
		// this can only be done in maps and lists (and lists, only with a numeric index).
		Value baseVal = baseSeq.Val(context);
		if (baseVal instanceof ValMap) {
			Pair<Value, ValMap> result = ((ValMap)baseVal).LookupPair(idxVal);
			if (result.getSecond() == null) throw new KeyException(idxVal.CodeForm(context.vm, 1));
			return result;
		} else if (baseVal instanceof ValList && idxVal instanceof ValNumber) {
			return new Pair<Value, ValMap>(((ValList) baseVal).GetElem(idxVal), null);
		} else if (baseVal instanceof ValString && idxVal instanceof ValNumber) {
			return new Pair<Value, ValMap>(((ValString) baseVal).GetElem(idxVal), null);
		}
			
		throw new TypeException("Type Exception: can't index into this type");
	}

	@Override
	public String ToString(Machine vm) {
		return String.format("%s%s%s", noInvoke ? "@" : "", sequence.ToString(), index.ToString());
	}

	@Override
	public int Hash(int recursionDepth) {
		return sequence.Hash(recursionDepth-1) ^ index.Hash(recursionDepth-1);
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		return rhs instanceof ValSeqElem && ((ValSeqElem)rhs).sequence == sequence
			&& ((ValSeqElem)rhs).index == index ? 1 : 0;
	}
}
