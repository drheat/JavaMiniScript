package com.catalinionescu.miniscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.catalinionescu.miniscript.exceptions.Check;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.LimitExceededException;
import com.catalinionescu.miniscript.exceptions.SourceLoc;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.intrinsics.Intrinsic;
import com.catalinionescu.miniscript.intrinsics.Result;
import com.catalinionescu.miniscript.types.Function;
import com.catalinionescu.miniscript.types.ValFunction;
import com.catalinionescu.miniscript.types.ValList;
import com.catalinionescu.miniscript.types.ValMap;
import com.catalinionescu.miniscript.types.ValNumber;
import com.catalinionescu.miniscript.types.ValSeqElem;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.Value;

public class Line {
	public enum Op {
		Noop,
		AssignA,
		AssignImplicit,
		APlusB,
		AMinusB,
		ATimesB,
		ADividedByB,
		AModB,
		APowB,
		AEqualB,
		ANotEqualB,
		AGreaterThanB,
		AGreatOrEqualB,
		ALessThanB,
		ALessOrEqualB,
		AisaB,
		AAndB,
		AOrB,
		BindAssignA,
		CopyA,
		NotA,
		GotoA,
		GotoAifB,
		GotoAifTrulyB,
		GotoAifNotB,
		PushParam,
		CallFunctionA,
		CallIntrinsicA,
		ReturnA,
		ElemBofA,
		ElemBofIterA,
		LengthOfA
	}
	
	public Value lhs;
	public Op op;
	public Value rhsA;
	public Value rhsB;
//	public string comment;
	public SourceLoc location;
	
	public Line(Value lhs, Op op) {
		this(lhs, op, null, null);
	}
	
	public Line(Value lhs, Op op, Value rhsA) {
		this(lhs, op, rhsA, null);
	}

	public Line(Value lhs, Op op, Value rhsA, Value rhsB) {
		this.lhs = lhs;
		this.op = op;
		this.rhsA = rhsA;
		this.rhsB = rhsB;
	}
	
	@Override
	public int hashCode() {
		return lhs.hashCode() ^ op.hashCode() ^ rhsA.hashCode() ^ rhsB.hashCode() ^ location.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Line)) return false;
		Line b = (Line)obj;
		return op == b.op && lhs == b.lhs && rhsA == b.rhsA && rhsB == b.rhsB && location == b.location;
	}
	
	@Override
	public String toString() {
		String text;
		switch (op) {
			case AssignA:
				text = String.format("%s := %s", lhs, rhsA);
				break;
			case AssignImplicit:
				text = String.format("_ := %s", rhsA);
				break;
			case APlusB:
				text = String.format("%s := %s + %s", lhs, rhsA, rhsB);
				break;
			case AMinusB:
				text = String.format("%s := %s - %s", lhs, rhsA, rhsB);
				break;
			case ATimesB:
				text = String.format("%s := %s * %s", lhs, rhsA, rhsB);
				break;
			case ADividedByB:
				text = String.format("%s := %s / %s", lhs, rhsA, rhsB);
				break;
			case AModB:
				text = String.format("%s := %s % %s", lhs, rhsA, rhsB);
				break;
			case APowB:
				text = String.format("%s := %s ^ %s", lhs, rhsA, rhsB);
				break;
			case AEqualB:
				text = String.format("%s := %s == %s", lhs, rhsA, rhsB);
				break;
			case ANotEqualB:
				text = String.format("%s := %s != %s", lhs, rhsA, rhsB);
				break;
			case AGreaterThanB:
				text = String.format("%s := %s > %s", lhs, rhsA, rhsB);
				break;
			case AGreatOrEqualB:
				text = String.format("%s := %s >= %s", lhs, rhsA, rhsB);
				break;
			case ALessThanB:
				text = String.format("%s := %s < %s", lhs, rhsA, rhsB);
				break;
			case ALessOrEqualB:
				text = String.format("%s := %s <= %s", lhs, rhsA, rhsB);
				break;
			case AAndB:
				text = String.format("%s := %s and %s", lhs, rhsA, rhsB);
				break;
			case AOrB:
				text = String.format("%s := %s or %s", lhs, rhsA, rhsB);
				break;
			case AisaB:
				text = String.format("%s := %s isa %s", lhs, rhsA, rhsB);
				break;
			case BindAssignA:
				text = String.format("%s := %s; %s.outerVars=", rhsA, rhsB);
				break;
			case CopyA:
				text = String.format("%s := copy of %s", lhs, rhsA);
				break;
			case NotA:
				text = String.format("%s := not %s", lhs, rhsA);
				break;
			case GotoA:
				text = String.format("goto %s", rhsA);
				break;
			case GotoAifB:
				text = String.format("goto %s if %s", rhsA, rhsB);
				break;
			case GotoAifTrulyB:
				text = String.format("goto %s if truly %s", rhsA, rhsB);
				break;
			case GotoAifNotB:
				text = String.format("goto %s if not %s", rhsA, rhsB);
				break;
			case PushParam:
				text = String.format("push param %s", rhsA);
				break;
			case CallFunctionA:
				text = String.format("%s := call %s with %s args", lhs, rhsA, rhsB);
				break;
			case CallIntrinsicA:
				text = String.format("intrinsic %s", Intrinsic.GetByID(rhsA.IntValue()));
				break;
			case ReturnA:
				text = String.format("%s := %s; return", lhs, rhsA);
				break;
			case ElemBofA:
				text = String.format("%s = %s[%s]", lhs, rhsA, rhsB);
				break;
			case ElemBofIterA:
				text = String.format("%s = %s iter %s", lhs, rhsA, rhsB);
				break;
			case LengthOfA:
				text = String.format("%s = len(%s)", lhs, rhsA);
				break;
			default:
				throw new RuntimeException("unknown opcode: " + op);
			
		}
		//if (comment != null) text = text + "\t// " + comment;
		if (location != null) text = text + "\t// " + location;
		return text;
	}

	/// <summary>
	/// Evaluate this line and return the value that would be stored
	/// into the lhs.
	/// </summary>
	public Value Evaluate(Context context) throws IndexException, KeyException, TypeException, LimitExceededException, UndefinedIdentifierException {
		if (op == Op.AssignA || op == Op.ReturnA || op == Op.AssignImplicit) {
			// Assignment is a bit of a special case.  It's EXTREMELY common
			// in TAC, so needs to be efficient, but we have to watch out for
			// the case of a RHS that is a list or map.  This means it was a
			// literal in the source, and may contain references that need to
			// be evaluated now.
			if (rhsA instanceof ValList || rhsA instanceof ValMap) {
				return rhsA.FullEval(context);
			} else if (rhsA == null) {
				return null;
			} else {
				return rhsA.Val(context);
			}
		}
		if (op == Op.CopyA) {
			// This opcode is used for assigning a literal.  We actually have
			// to copy the literal, in the case of a mutable object like a
			// list or map, to ensure that if the same code executes again,
			// we get a new, unique object.
			if (rhsA instanceof ValList) {
				return ((ValList)rhsA).EvalCopy(context);
			} else if (rhsA instanceof ValMap) {
				return ((ValMap)rhsA).EvalCopy(context);
			} else if (rhsA == null) {
				return null;
			} else {
				return rhsA.Val(context);
			}
		}

		Value opA = rhsA!=null ? rhsA.Val(context) : null;
		Value opB = rhsB!=null ? rhsB.Val(context) : null;

		if (op == Op.AisaB) {
			if (opA == null) return ValNumber.Truth(opB == null);
			return ValNumber.Truth(opA.IsA(opB, context.vm));
		}

		if (op == Op.ElemBofA && opB instanceof ValString) {
			// You can now look for a string in almost anything...
			// and we have a convenient (and relatively fast) method for it:
			return ValSeqElem.Resolve(opA, ((ValString)opB).value, context).getFirst();
		}

		// check for special cases of comparison to null (works with any type)
		if (op == Op.AEqualB && (opA == null || opB == null)) {
			return ValNumber.Truth(opA == opB);
		}
		if (op == Op.ANotEqualB && (opA == null || opB == null)) {
			return ValNumber.Truth(opA != opB);
		}
		
		// check for implicit coersion of other types to string; this happens
		// when either side is a string and the operator is addition.
		if ((opA instanceof ValString || opB instanceof ValString) && op == Op.APlusB) {
			if (opA == null) return opB;
			if (opB == null) return opA;
			String sA = opA.ToString(context.vm);
			String sB = opB.ToString(context.vm);
			if (sA.length() + sB.length() > ValString.maxSize) throw new LimitExceededException("string too large");
			return new ValString(sA + sB);
		}

		if (opA instanceof ValNumber) {
			double fA = ((ValNumber)opA).value;
			switch (op) {
				case GotoA:
					context.lineNum = (int)fA;
					return null;
				case GotoAifB:
					if (opB != null && opB.BoolValue()) context.lineNum = (int)fA;
					return null;
				case GotoAifTrulyB:
					{
						// Unlike GotoAifB, which branches if B has any nonzero
						// value (including 0.5 or 0.001), this branches only if
						// B is TRULY true, i.e., its integer value is nonzero.
						// (Used for short-circuit evaluation of "or".)
						int i = 0;
						if (opB != null) i = opB.IntValue();
						if (i != 0) context.lineNum = (int)fA;
						return null;
					}
				case GotoAifNotB:
					if (opB == null || !opB.BoolValue()) context.lineNum = (int)fA;
					return null;
				case CallIntrinsicA:
					// NOTE: intrinsics do not go through NextFunctionContext.  Instead
					// they execute directly in the current context.  (But usually, the
					// current context is a wrapper function that was invoked via
					// Op.CallFunction, so it got a parameter context at that time.)
					Result result = Intrinsic.Execute((int)fA, context, context.partialResult);
					if (result.done) {
						context.partialResult = null;
						return result.result;
					}
					// OK, this intrinsic function is not yet done with its work.
					// We need to stay on this same line and call it again with 
					// the partial result, until it reports that its job is complete.
					context.partialResult = result;
					context.lineNum--;
					return null;
				case NotA:
					return new ValNumber(1.0 - AbsClamp01(fA));
				default:
					// keep compiler happy
					break;
			}
			if (opB instanceof ValNumber || opB == null) {
				double fB = opB != null ? ((ValNumber)opB).value : 0;
				switch (op) {
				case APlusB:
					return new ValNumber(fA + fB);
				case AMinusB:
					return new ValNumber(fA - fB);
				case ATimesB:
					return new ValNumber(fA * fB);
				case ADividedByB:
					return new ValNumber(fA / fB);
				case AModB:
					return new ValNumber(fA % fB);
				case APowB:
					return new ValNumber(Math.pow(fA, fB));
				case AEqualB:
					return ValNumber.Truth(fA == fB);
				case ANotEqualB:
					return ValNumber.Truth(fA != fB);
				case AGreaterThanB:
					return ValNumber.Truth(fA > fB);
				case AGreatOrEqualB:
					return ValNumber.Truth(fA >= fB);
				case ALessThanB:
					return ValNumber.Truth(fA < fB);
				case ALessOrEqualB:
					return ValNumber.Truth(fA <= fB);
				case AAndB:
					if (!(opB instanceof ValNumber)) fB = opB != null && opB.BoolValue() ? 1 : 0;
					return new ValNumber(AbsClamp01(fA * fB));
				case AOrB:
					if (!(opB instanceof ValNumber)) fB = opB != null && opB.BoolValue() ? 1 : 0;
					return new ValNumber(AbsClamp01(fA + fB - fA * fB));
				default:
					break;
				}
			}
			// Handle equality testing between a number (opA) and a non-number (opB).
			// These are always considered unequal.
			if (op == Op.AEqualB) return ValNumber.zero;
			if (op == Op.ANotEqualB) return ValNumber.one;

		} else if (opA instanceof ValString) {
			String sA = ((ValString)opA).value;
			if (op == Op.ATimesB || op == Op.ADividedByB) {
				double factor = 0;
				if (op == Op.ATimesB) {
					Check.Type(opB, ValNumber.class, "string replication");
					factor = ((ValNumber)opB).value;
				} else {
					Check.Type(opB, ValNumber.class, "string division");
					factor = 1.0 / ((ValNumber)opB).value;								
				}
				int repeats = (int)factor;
				if (repeats < 0) return ValString.empty;
				if (repeats * sA.length() > ValString.maxSize) throw new LimitExceededException("string too large");
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < repeats; i++) result.append(sA);
				int extraChars = (int)(sA.length() * (factor - repeats));
				if (extraChars > 0) result.append(sA.substring(0, extraChars));
				return new ValString(result.toString());						
			}
			if (op == Op.ElemBofA || op == Op.ElemBofIterA) {
				int idx = opB.IntValue();
				Check.Range(idx, -sA.length(), sA.length() - 1, "string index");
				if (idx < 0) idx += sA.length();
				return new ValString(sA.substring(idx, idx + 1));
			}
			if (opB == null || opB instanceof ValString) {
				String sB = (opB == null ? null : opB.ToString(context.vm));
				switch (op) {
					case AMinusB: {
							if (opB == null) return opA;
							if (sA.endsWith(sB)) sA = sA.substring(0, sA.length() - sB.length());
							return new ValString(sA);
						}
					case NotA:
						return ValNumber.Truth(sA == null || sA.isEmpty());
					case AEqualB:
						return ValNumber.Truth((sA == null && sB == null) || sA.equals(sB));
					case ANotEqualB:
						return ValNumber.Truth(!((sA == null && sB == null) || sA.equals(sB)));
					case AGreaterThanB:
						return ValNumber.Truth(sA.compareTo(sB) > 0);		// TODO: null check
					case AGreatOrEqualB:
						return ValNumber.Truth(sA.compareTo(sB) >= 0);		// TODO: null check
					case ALessThanB:
						int foo = sA.compareTo(sB);							// TODO: null check
						return ValNumber.Truth(foo < 0);
					case ALessOrEqualB:
						return ValNumber.Truth(sA.compareTo(sB) <= 0);		// TODO: null check
					case LengthOfA:
						return new ValNumber(sA.length());
					default:
						break;
				}
			} else {
				// RHS is neither null nor a string.
				// We no longer automatically coerce in all these cases; about
				// all we can do is equal or unequal testing.
				// (Note that addition was handled way above here.)
				if (op == Op.AEqualB) return ValNumber.zero;
				if (op == Op.ANotEqualB) return ValNumber.one;						
			}
		} else if (opA instanceof ValList) {
			List<Value> list = ((ValList)opA).values;
			if (op == Op.ElemBofA || op == Op.ElemBofIterA) {
				// list indexing
				int idx = opB.IntValue();
				Check.Range(idx, -list.size(), list.size() - 1, "list index");
				if (idx < 0) idx += list.size();
				return list.get(idx);
			} else if (op == Op.LengthOfA) {
				return new ValNumber(list.size());
			} else if (op == Op.AEqualB) {
				return ValNumber.Truth(((ValList) opA).Equality(opB));
			} else if (op == Op.ANotEqualB) {
				return ValNumber.Truth(1.0 - ((ValList) opA).Equality(opB));
			} else if (op == Op.APlusB) {
				// list concatenation
				Check.Type(opB, ValList.class, "list concatenation");
				List<Value> list2 = ((ValList)opB).values;
				if (list.size() + list2.size() > ValList.maxSize) throw new LimitExceededException("list too large");
				List<Value> result = new ArrayList<>(list.size() + list2.size());
				for (Value v : list) result.add(context.ValueInContext(v));
				for (Value v : list2) result.add(context.ValueInContext(v));
				return new ValList(result);
			} else if (op == Op.ATimesB || op == Op.ADividedByB) {
				// list replication (or division)
				double factor = 0;
				if (op == Op.ATimesB) {
					Check.Type(opB, ValNumber.class, "list replication");
					factor = ((ValNumber)opB).value;
				} else {
					Check.Type(opB, ValNumber.class, "list division");
					factor = 1.0 / ((ValNumber)opB).value;								
				}
				if (factor <= 0) return new ValList();
				int finalCount = (int)(list.size() * factor);
				if (finalCount > ValList.maxSize) throw new LimitExceededException("list too large");
				List<Value> result = new ArrayList<>(finalCount);
				for (int i = 0; i < finalCount; i++) {
					result.add(context.ValueInContext(list.get(i % list.size())));
				}
				return new ValList(result);
			} else if (op == Op.NotA) {
				return ValNumber.Truth(!opA.BoolValue());
			}
		} else if (opA instanceof ValMap) {
			if (op == Op.ElemBofA) {
				// map lookup
				// (note, cases where opB is a string are handled above, along with
				// all the other types; so we'll only get here for non-string cases)
				ValSeqElem se = new ValSeqElem(opA, opB);
				return se.Val(context);
				// (This ensures we walk the "__isa" chain in the standard way.)
			} else if (op == Op.ElemBofIterA) {
				// With a map, ElemBofIterA is different from ElemBofA.  This one
				// returns a mini-map containing a key/value pair.
				return ((ValMap)opA).GetKeyValuePair(opB.IntValue());
			} else if (op == Op.LengthOfA) {
				return new ValNumber(((ValMap)opA).Count());
			} else if (op == Op.AEqualB) {
				return ValNumber.Truth(((ValMap)opA).Equality(opB));
			} else if (op == Op.ANotEqualB) {
				return ValNumber.Truth(1.0 - ((ValMap)opA).Equality(opB));
			} else if (op == Op.APlusB) {
				// map combination
				Map<Value, Value> map = ((ValMap)opA).map;
				Check.Type(opB, ValMap.class, "map combination");
				Map<Value, Value> map2 = ((ValMap)opB).map;
				ValMap result = new ValMap();
				for (Entry<Value, Value> kv : map.entrySet()) result.map.put(kv.getKey(), context.ValueInContext(kv.getValue()));
				for (Entry<Value, Value> kv : map2.entrySet()) result.map.put(kv.getKey(), context.ValueInContext(kv.getValue()));
				return result;
			} else if (op == Op.NotA) {
				return ValNumber.Truth(!opA.BoolValue());
			}
		} else if (opA instanceof ValFunction && opB instanceof ValFunction) {
			Function fA = ((ValFunction)opA).function;
			Function fB = ((ValFunction)opB).function;
			switch (op) {
				case AEqualB:
					return ValNumber.Truth(fA == fB);
				case ANotEqualB:
					return ValNumber.Truth(fA != fB);
				default:
					// keep compiler happy
					break;
			}
		} else {
			// opA is something else... perhaps null
			switch (op) {
				case BindAssignA:
					if (context.variables == null) context.variables = new ValMap();
					ValFunction valFunc = (ValFunction)opA;
                    return valFunc.BindAndCopy(context.variables);
				case NotA:
					return opA != null && opA.BoolValue() ? ValNumber.zero : ValNumber.one;
				default:
					// keep compiler happy
					break;
			}
		}
		

		if (op == Op.AAndB || op == Op.AOrB) {
			// We already handled the case where opA was a number above;
			// this code handles the case where opA is something else.
			double fA = opA != null && opA.BoolValue() ? 1 : 0;
			double fB;
			if (opB instanceof ValNumber) fB = ((ValNumber)opB).value;
			else fB = opB != null && opB.BoolValue() ? 1 : 0;
			double result;
			if (op == Op.AAndB) {
				result = AbsClamp01(fA * fB);
			} else {
				result = AbsClamp01(fA + fB - fA * fB);
			}
			return new ValNumber(result);
		}
		return null;
	}

	static double AbsClamp01(double d) {
		if (d < 0) d = -d;
		if (d > 1) return 1;
		return d;
	}
}
