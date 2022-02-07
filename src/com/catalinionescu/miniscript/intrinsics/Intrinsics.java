package com.catalinionescu.miniscript.intrinsics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.catalinionescu.miniscript.Line;
import com.catalinionescu.miniscript.TAC;
import com.catalinionescu.miniscript.exceptions.Check;
import com.catalinionescu.miniscript.exceptions.LimitExceededException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.types.ValFunction;
import com.catalinionescu.miniscript.types.ValList;
import com.catalinionescu.miniscript.types.ValMap;
import com.catalinionescu.miniscript.types.ValNull;
import com.catalinionescu.miniscript.types.ValNumber;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.Value;
import com.catalinionescu.miniscript.types.ValueReverseSorter;
import com.catalinionescu.miniscript.types.ValueSorter;
import com.catalinionescu.miniscript.utils.StringUtil;
import com.catalinionescu.miniscript.utils.Unicode;
import com.catalinionescu.miniscript.utils.fastmath.Precision;

/// <summary>
/// Intrinsics: a static class containing all of the standard MiniScript
/// built-in intrinsics.  You shouldn't muck with these, but feel free
/// to browse them for lots of examples of how to write your own intrinics.
/// </summary>
public class Intrinsics {
	static boolean initialized;	

	/// <summary>
	/// InitIfNeeded: called automatically during script setup to make sure
	/// that all our standard intrinsics are defined.  Note how we use a
	/// private bool flag to ensure that we don't create our intrinsics more
	/// than once, no matter how many times this method is called.
	/// </summary>
	public static void InitIfNeeded() {
		if (initialized) return;	// our work is already done; bail out.
		initialized = true;
		Intrinsic f;

		// abs
		//	Returns the absolute value of the given number.
		// x (number, default 0): number to take the absolute value of.
		// Example: abs(-42)		returns 42
		f = Intrinsic.Create("abs");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.abs(context.GetLocalDouble("x")));
		};

		// acos
		//	Returns the inverse cosine, that is, the angle 
		//	(in radians) whose cosine is the given value.
		// x (number, default 0): cosine of the angle to find.
		// Returns: angle, in radians, whose cosine is x.
		// Example: acos(0) 		returns 1.570796
		f = Intrinsic.Create("acos");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.acos(context.GetLocalDouble("x")));
		};

		// asin
		//	Returns the inverse sine, that is, the angle
		//	(in radians) whose sine is the given value.
		// x (number, default 0): cosine of the angle to find.
		// Returns: angle, in radians, whose cosine is x.
		// Example: asin(1) return 1.570796
		f = Intrinsic.Create("asin");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.asin(context.GetLocalDouble("x")));
		};

		// atan
		//	Returns the arctangent of a value or ratio, that is, the
		//	angle (in radians) whose tangent is y/x.  This will return
		//	an angle in the correct quadrant, taking into account the
		//	sign of both arguments.  The second argument is optional,
		//	and if omitted, this function is equivalent to the traditional
		//	one-parameter atan function.  Note that the parameters are
		//	in y,x order.
		// y (number, default 0): height of the side opposite the angle
		// x (number, default 1): length of the side adjacent the angle
		// Returns: angle, in radians, whose tangent is y/x
		// Example: atan(1, -1)		returns 2.356194
		f = Intrinsic.Create("atan");
		f.AddParam("y", 0);
		f.AddParam("x", 1);
		f.code = (context, partialResult) -> {
			double y = context.GetLocalDouble("y");
			double x = context.GetLocalDouble("x");
			if (x == 1.0) return new Result(Math.atan(y));
			return new Result(Math.atan2(y, x));
		};

		// bitAnd
		//	Treats its arguments as integers, and computes the bitwise
		//	`and`: each bit in the result is set only if the corresponding
		//	bit is set in both arguments.
		// i (number, default 0): first integer argument
		// j (number, default 0): second integer argument
		// Returns: bitwise `and` of i and j
		// Example: bitAnd(14, 7)		returns 6
		// See also: bitOr; bitXor
		f = Intrinsic.Create("bitAnd");
		f.AddParam("i", 0);
		f.AddParam("j", 0);
		f.code = (context, partialResult) -> {
			int i = context.GetLocalInt("i");
			int j = context.GetLocalInt("j");
			return new Result(i & j);
		};
		
		// bitOr
		//	Treats its arguments as integers, and computes the bitwise
		//	`or`: each bit in the result is set if the corresponding
		//	bit is set in either (or both) of the arguments.
		// i (number, default 0): first integer argument
		// j (number, default 0): second integer argument
		// Returns: bitwise `or` of i and j
		// Example: bitOr(14, 7)		returns 15
		// See also: bitAnd; bitXor
		f = Intrinsic.Create("bitOr");
		f.AddParam("i", 0);
		f.AddParam("j", 0);
		f.code = (context, partialResult) -> {
			int i = context.GetLocalInt("i");
			int j = context.GetLocalInt("j");
			return new Result(i | j);
		};
		
		// bitXor
		//	Treats its arguments as integers, and computes the bitwise
		//	`xor`: each bit in the result is set only if the corresponding
		//	bit is set in exactly one (not zero or both) of the arguments.
		// i (number, default 0): first integer argument
		// j (number, default 0): second integer argument
		// Returns: bitwise `xor` of i and j
		// Example: bitXor(14, 7)		returns 9
		// See also: bitAnd; bitOr
		f = Intrinsic.Create("bitXor");
		f.AddParam("i", 0);
		f.AddParam("j", 0);
		f.code = (context, partialResult) -> {
			int i = context.GetLocalInt("i");
			int j = context.GetLocalInt("j");
			return new Result(i ^ j);
		};
		
		// char
		//	Gets a character from its Unicode code point.
		// codePoint (number, default 65): Unicode code point of a character
		// Returns: string containing the specified character
		// Example: char(42)		returns "*"
		// See also: code
		f = Intrinsic.Create("char");
		f.AddParam("codePoint", 65);
		f.code = (context, partialResult) -> {
			int codepoint = context.GetLocalInt("codePoint");
			String s = Unicode.codepointToString(codepoint);
			return new Result(s);
		};
		
		// ceil
		//	Returns the "ceiling", i.e. closest whole number 
		//	greater than or equal to the given number.
		// x (number, default 0): number to get the ceiling of
		// Returns: closest whole number not less than x
		// Example: ceil(41.2)		returns 42
		// See also: floor
		f = Intrinsic.Create("ceil");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.ceil(context.GetLocalDouble("x")));
		};
		
		// code
		//	Return the Unicode code point of the first character of
		//	the given string.  This is the inverse of `char`.
		//	May be called with function syntax or dot syntax.
		// self (string): string to get the code point of
		// Returns: Unicode code point of the first character of self
		// Example: "*".code		returns 42
		// Example: code("*")		returns 42
		f = Intrinsic.Create("code");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			int codepoint = 0;			
			if (self != null) codepoint = Character.codePointAt(self.toString(), 0);
			return new Result(codepoint);
		};
					
		// cos
		//	Returns the cosine of the given angle (in radians).
		// radians (number): angle, in radians, to get the cosine of
		// Returns: cosine of the given angle
		// Example: cos(0)		returns 1
		f = Intrinsic.Create("cos");
		f.AddParam("radians", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.cos(context.GetLocalDouble("radians")));
		};

		// floor
		//	Returns the "floor", i.e. closest whole number 
		//	less than or equal to the given number.
		// x (number, default 0): number to get the floor of
		// Returns: closest whole number not more than x
		// Example: floor(42.9)		returns 42
		// See also: floor
		f = Intrinsic.Create("floor");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.floor(context.GetLocalDouble("x")));
		};

		// funcRef
		//	Returns a map that represents a function reference in
		//	MiniScript's core type system.  This can be used with `isa`
		//	to check whether a variable refers to a function (but be
		//	sure to use @ to avoid invoking the function and testing
		//	the result).
		// Example: @floor isa funcRef		returns 1
		// See also: number, string, list, map
		f = Intrinsic.Create("funcRef");
		f.code = (context, partialResult) -> {
			if (context.vm.functionType == null) {
				context.vm.functionType = FunctionType().EvalCopy(context.vm.globalContext);
			}
			return new Result(context.vm.functionType);
		};
		
		// hash
		//	Returns an integer that is "relatively unique" to the given value.
		//	In the case of strings, the hash is case-sensitive.  In the case
		//	of a list or map, the hash combines the hash values of all elements.
		//	Note that the value returned is platform-dependent, and may vary
		//	across different MiniScript implementations.
		// obj (any type): value to hash
		// Returns: integer hash of the given value
		f = Intrinsic.Create("hash");
		f.AddParam("obj");
		f.code = (context, partialResult) -> {
			Value val = context.GetLocal("obj");
			return new Result(val.Hash());
		};

		// hasIndex
		//	Return whether the given index is valid for this object, that is,
		//	whether it could be used with square brackets to get some value
		//	from self.  When self is a list or string, the result is true for
		//	integers from -(length of string) to (length of string-1).  When
		//	self is a map, it is true for any key (index) in the map.  If
		//	called on a number, this method throws a runtime exception.
		// self (string, list, or map): object to check for an index on
		// index (any): value to consider as a possible index
		// Returns: 1 if self[index] would be valid; 0 otherwise
		// Example: "foo".hasIndex(2)		returns 1
		// Example: "foo".hasIndex(3)		returns 0
		// See also: indexes
		f = Intrinsic.Create("hasIndex");
		f.AddParam("self");
		f.AddParam("index");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			Value index = context.GetLocal("index");
			if (self instanceof ValList) {
				if (!(index instanceof ValNumber)) return Result.False;	// #3
				List<Value> list = ((ValList)self).values;
				int i = index.IntValue();
				return new Result(ValNumber.Truth(i >= -list.size() && i < list.size()));
			} else if (self instanceof ValString) {
				String str = ((ValString)self).value;
				int i = index.IntValue();
				return new Result(ValNumber.Truth(i >= -str.length() && i < str.length()));
			} else if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				return new Result(ValNumber.Truth(map.ContainsKey(index)));
			}
			return Result.Null;
		};
		
		// indexes
		//	Returns the keys of a dictionary, or the non-negative indexes
		//	for a string or list.
		// self (string, list, or map): object to get the indexes of
		// Returns: a list of valid indexes for self
		// Example: "foo".indexes		returns [0, 1, 2]
		// See also: hasIndex
		f = Intrinsic.Create("indexes");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				List<Value> keys = new ArrayList<>(map.map.keySet());
				for (int i = 0; i < keys.size(); i++) if (keys.get(i) instanceof ValNull) keys.set(i, null);
				return new Result(new ValList(keys));
			} else if (self instanceof ValString) {
				String str = ((ValString)self).value;
				List<Value> indexes = new ArrayList<Value>(str.length());
				for (int i = 0; i < str.length(); i++) {
					indexes.add(TAC.Num(i));
				}
				return new Result(new ValList(indexes));
			} else if (self instanceof ValList) {
				List<Value> list = ((ValList)self).values;
				List<Value> indexes = new ArrayList<Value>(list.size());
				for (int i = 0; i < list.size(); i++) {
					indexes.add(TAC.Num(i));
				}
				return new Result(new ValList(indexes));
			}
			return Result.Null;
		};
		
		// indexOf
		//	Returns index or key of the given value, or if not found,		returns null.
		// self (string, list, or map): object to search
		// value (any): value to search for
		// after (any, optional): if given, starts the search after this index
		// Returns: first index (after `after`) such that self[index] == value, or null
		// Example: "Hello World".indexOf("o")		returns 4
		// Example: "Hello World".indexOf("o", 4)		returns 7
		// Example: "Hello World".indexOf("o", 7)		returns null			
		f = Intrinsic.Create("indexOf");
		f.AddParam("self");
		f.AddParam("value");
		f.AddParam("after");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			Value value = context.GetLocal("value");
			Value after = context.GetLocal("after");
			if (self instanceof ValList) {
				List<Value> list = ((ValList) self).values;
				int idx = -1;
				if (after == null) {
					// idx = list.FindIndex(x -> x == null ? value == null : x.Equality(value) == 1);
					idx = 0;
					boolean found = false;
					for (Value x : list) {
						if (x == null) {
							if (value == null) {
								found = true;
								break;
							}
						} else if (x.Equality(value) == 1) {
							found = true;
							break;
						}
						
						idx++;
					}
					
					if (!found) idx = -1;
				} else {
					int afterIdx = after.IntValue();
					if (afterIdx < -1) afterIdx += list.size();
					if (afterIdx < -1 || afterIdx >= list.size()-1) return Result.Null;
					// idx = list.FindIndex(afterIdx + 1, x -> x == null ? value == null : x.Equality(value) == 1);
					boolean found = false;
					for (int i = afterIdx + 1; i < list.size(); i++) {
						Value x = list.get(i);
						if (x == null) {
							if (value == null) {
								idx = i;
								found = true;
								break;
							}
						} else if (x.Equality(value) == 1) {
							idx = i;
							found = true;
							break;
						}
					}
					if (!found) {
						idx = -1;
					}
				}
				if (idx >= 0) return new Result(idx);
			} else if (self instanceof ValString) {
				String str = ((ValString)self).value;
				if (value == null) return Result.Null;
				String s = value.toString();
				int idx;
				if (after == null) idx = str.indexOf(s);
				else {
					int afterIdx = after.IntValue();
					if (afterIdx < -1) afterIdx += str.length();
					if (afterIdx < -1 || afterIdx >= str.length()-1) return Result.Null;
					idx = str.indexOf(s, afterIdx + 1);
				}
				if (idx >= 0) return new Result(idx);
			} else if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				boolean sawAfter = (after == null);
				for (Value k : map.map.keySet()) {
					if (!sawAfter) {
						if (k.Equality(after) == 1) sawAfter = true;
					} else {
						if (map.map.get(k).Equality(value) == 1) return new Result(k);
					}
				}
			}
			return Result.Null;
		};

		// insert
		//	Insert a new element into a string or list.  In the case of a list,
		//	the list is both modified in place and returned.  Strings are immutable,
		//	so in that case the original string is unchanged, but a new string is
		//	returned with the value inserted.
		// self (string or list): sequence to insert into
		// index (number): position at which to insert the new item
		// value (any): element to insert at the specified index
		// Returns: modified list, new string
		// Example: "Hello".insert(2, 42)		returns "He42llo"
		// See also: remove
		f = Intrinsic.Create("insert");
		f.AddParam("self");
		f.AddParam("index");
		f.AddParam("value");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			Value index = context.GetLocal("index");
			Value value = context.GetLocal("value");
			if (index == null) throw new RuntimeException("insert: index argument required");
			if (!(index instanceof ValNumber)) throw new RuntimeException("insert: number required for index argument");
			int idx = index.IntValue();
			if (self instanceof ValList) {
				List<Value> list = ((ValList)self).values;
				if (idx < 0) idx += list.size() + 1;	// +1 because we are inserting AND counting from the end.
				Check.Range(idx, 0, list.size());	// and allowing all the way up to .Count here, because insert.
				list.add(idx, value);
				return new Result(self);
			} else if (self instanceof ValString) {
				String s = self.toString();
				if (idx < 0) idx += s.length() + 1;
				Check.Range(idx, 0, s.length());
				s = s.substring(0, idx) + value.toString() + s.substring(idx);
				return new Result(s);
			} else {
				throw new RuntimeException("insert called on invalid type");
			}
		};

		// self.join
		//	Join the elements of a list together to form a string.
		// self (list): list to join
		// delimiter (string, default " "): string to insert between each pair of elements
		// Returns: string built by joining elements of self with delimiter
		// Example: [2,4,8].join("-")		returns "2-4-8"
		// See also: split
		f = Intrinsic.Create("join");
		f.AddParam("self");
		f.AddParam("delimiter", " ");
		f.code = (context, partialResult) -> {
			Value val = context.self;
			String delim = context.GetLocalString("delimiter");
			if (!(val instanceof ValList)) return new Result(val);
			ValList src = (ValList) val;
			List<String> list = new ArrayList<>(src.values.size());
			for (int i=0; i<src.values.size(); i++) {
				if (src.values.get(i) == null) list.add(null);
				else list.add(src.values.get(i).toString());
			}
			String result = String.join(delim, list);
			return new Result(result);
		};
		
		// self.len
		//	Return the number of characters in a string, elements in
		//	a list, or key/value pairs in a map.
		//	May be called with function syntax or dot syntax.
		// self (list, string, or map): object to get the length of
		// Returns: length (number of elements) in self
		// Example: "hello".len		returns 5
		f = Intrinsic.Create("len");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value val = context.self;
			if (val instanceof ValList) {
				List<Value> list = ((ValList)val).values;
				return new Result(list.size());
			} else if (val instanceof ValString) {
				String str = ((ValString)val).value;
				return new Result(str.length());
			} else if (val instanceof ValMap) {
				return new Result(((ValMap)val).Count());
			}
			return Result.Null;
		};
		
		// list type
		//	Returns a map that represents the list datatype in
		//	MiniScript's core type system.  This can be used with `isa`
		//	to check whether a variable refers to a list.  You can also
		//	assign new methods here to make them available to all lists.
		// Example: [1, 2, 3] isa list		returns 1
		// See also: number, string, map, funcRef
		f = Intrinsic.Create("list");
		f.code = (context, partialResult) -> {
			if (context.vm.listType == null) {
				context.vm.listType = ListType().EvalCopy(context.vm.globalContext);
			}
			return new Result(context.vm.listType);
		};
		
		// log(x, base)
		//	Returns the logarithm (with the given) of the given number,
		//	that is, the number y such that base^y = x.
		// x (number): number to take the log of
		// base (number, default 10): logarithm base
		// Returns: a number that, when base is raised to it, produces x
		// Example: log(1000)		returns 3 (because 10^3 == 1000)
		f = Intrinsic.Create("log");
		f.AddParam("x", 0);
		f.AddParam("base", 10);
		f.code = (context, partialResult) -> {
			double x = context.GetLocalDouble("x");
			double b = context.GetLocalDouble("base");
			double result;
			if (Math.abs(b - 2.718282) < 0.000001) result = Math.log(x);
			else result = Math.log(x) / Math.log(b);
			return new Result(result);
		};
		
		// lower
		//	Return a lower-case version of a string.
		//	May be called with function syntax or dot syntax.
		// self (string): string to lower-case
		// Returns: string with all capital letters converted to lowercase
		// Example: "Mo Spam".lower		returns "mo spam"
		// See also: upper
		f = Intrinsic.Create("lower");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value val = context.self;
			if (val instanceof ValString) {
				String str = ((ValString)val).value;
				return new Result(str.toLowerCase());
			}
			return new Result(val);
		};

		// map type
		//	Returns a map that represents the map datatype in
		//	MiniScript's core type system.  This can be used with `isa`
		//	to check whether a variable refers to a map.  You can also
		//	assign new methods here to make them available to all maps.
		// Example: {1:"one"} isa map		returns 1
		// See also: number, string, list, funcRef
		f = Intrinsic.Create("map");
		f.code = (context, partialResult) -> {
			if (context.vm.mapType == null) {
				context.vm.mapType = MapType().EvalCopy(context.vm.globalContext);
			}
			return new Result(context.vm.mapType);
		};
		
		// number type
		//	Returns a map that represents the number datatype in
		//	MiniScript's core type system.  This can be used with `isa`
		//	to check whether a variable refers to a number.  You can also
		//	assign new methods here to make them available to all maps
		//	(though because of a limitation in MiniScript's parser, such
		//	methods do not work on numeric literals).
		// Example: 42 isa number		returns 1
		// See also: string, list, map, funcRef
		f = Intrinsic.Create("number");
		f.code = (context, partialResult) -> {
			if (context.vm.numberType == null) {
				context.vm.numberType = NumberType().EvalCopy(context.vm.globalContext);
			}
			return new Result(context.vm.numberType);
		};
		
		// pi
		//	Returns the universal constant pi, that is, the ratio of
		//	a circle's circumference to its diameter.
		// Example: pi		returns 3.141593
		f = Intrinsic.Create("pi");
		f.code = (context, partialResult) -> {
			return new Result(Math.PI);
		};

		// print
		//	Display the given value on the default output stream.  The
		//	exact effect may vary with the environment.  In most cases, the
		//	given string will be followed by the standard line delimiter.
		// s (any): value to print (converted to a string as needed)
		// Returns: null
		// Example: print 6*7
		f = Intrinsic.Create("print");
		f.AddParam("s", ValString.empty);
		f.code = (context, partialResult) -> {
			Value s = context.GetLocal("s");
			if (s != null) context.vm.standardOutput.print(s.toString());
			else context.vm.standardOutput.print("null");
			return Result.Null;
		};
			
		// pop
		//	Removes and	returns the last item in a list, or an arbitrary
		//	key of a map.  If the list or map is empty (or if called on
		//	any other data type), returns null.
		//	May be called with function syntax or dot syntax.
		// self (list or map): object to remove an element from the end of
		// Returns: value removed, or null
		// Example: [1, 2, 3].pop		returns (and removes) 3
		// See also: pull; push; remove
		f = Intrinsic.Create("pop");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			if (self instanceof ValList) {
				List<Value> list = ((ValList)self).values;
				if (list.size() < 1) return Result.Null;
				Value result = list.get(list.size() - 1);
				list.remove(list.size() - 1);
				return new Result(result);
			} else if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				if (map.map.size() < 1) return Result.Null;
				Value result = map.map.keySet().iterator().next();
				map.map.remove(result);
				return new Result(result);
			}
			return Result.Null;
		};

		// pull
		//	Removes and	returns the first item in a list, or an arbitrary
		//	key of a map.  If the list or map is empty (or if called on
		//	any other data type), returns null.
		//	May be called with function syntax or dot syntax.
		// self (list or map): object to remove an element from the end of
		// Returns: value removed, or null
		// Example: [1, 2, 3].pull		returns (and removes) 1
		// See also: pop; push; remove
		f = Intrinsic.Create("pull");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			if (self instanceof ValList) {
				List<Value> list = ((ValList)self).values;
				if (list.size() < 1) return Result.Null;
				Value result = list.get(0);
				list.remove(0);
				return new Result(result);
			} else if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				if (map.map.size() < 1) return Result.Null;
				Value result = map.map.keySet().iterator().next();
				map.map.remove(result);
				return new Result(result);
			}
			return Result.Null;
		};

		// push
		//	Appends an item to the end of a list, or inserts it into a map
		//	as a key with a value of 1.
		//	May be called with function syntax or dot syntax.
		// self (list or map): object to append an element to
		// Returns: self
		// See also: pop, pull, insert
		f = Intrinsic.Create("push");
		f.AddParam("self");
		f.AddParam("value");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			Value value = context.GetLocal("value");
			if (self instanceof ValList) {
				List<Value> list = ((ValList)self).values;
				list.add(value);
				return new Result(self);
			} else if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				map.map.put(value, ValNumber.one);
				return new Result(self);
			}
			return Result.Null;
		};

		// range
		//	Return a list containing a series of numbers within a range.
		// from (number, default 0): first number to include in the list
		// to (number, default 0): point at which to stop adding numbers to the list
		// step (number, optional): amount to add to the previous number on each step;
		//	defaults to 1 if to > from, or -1 if to < from
		// Example: range(50, 5, -10)		returns [50, 40, 30, 20, 10]
		f = Intrinsic.Create("range");
		f.AddParam("from", 0);
		f.AddParam("to", 0);
		f.AddParam("step");
		f.code = (context, partialResult) -> {
			Value p0 = context.GetLocal("from");
			Value p1 = context.GetLocal("to");
			Value p2 = context.GetLocal("step");
			double fromVal = p0.DoubleValue();
			double toVal = p1.DoubleValue();
			double step = (toVal >= fromVal ? 1 : -1);
			if (p2 instanceof ValNumber) step = ((ValNumber)p2).value;
			if (step == 0) throw new RuntimeException("range() error (step==0)");
			List<Value> values = new ArrayList<>();
			int count = (int)((toVal - fromVal) / step) + 1;
			if (count > ValList.maxSize) throw new RuntimeException("list too large");
			try {
				values = new ArrayList<>(count);
				for (double v = fromVal; step > 0 ? (v <= toVal) : (v >= toVal); v += step) {
					values.add(TAC.Num(v));
				}
			} catch (Exception e) {
				// uh-oh... probably out-of-memory exception; clean up and bail out
				values = null;
				throw(new LimitExceededException("range() error", e));
			}
			return new Result(new ValList(values));
		};

		// remove
		//	Removes part of a list, map, or string.  Exact behavior depends on
		//	the data type of self:
		// 		list: removes one element by its index; the list is mutated in place;
		//			returns null, and throws an error if the given index out of range
		//		map: removes one key/value pair by key; the map is mutated in place;
		//			returns 1 if key was found, 0 otherwise
		//		string:	returns a new string with the first occurrence of k removed
		//	May be called with function syntax or dot syntax.
		// self (list, map, or string): object to remove something from
		// k (any): index or substring to remove
		// Returns: (see above)
		// Example: a=["a","b","c"]; a.remove 1		leaves a == ["a", "c"]
		// Example: d={"ichi":"one"}; d.remove "ni"		returns 0
		// Example: "Spam".remove("S")		returns "pam"
		// See also: indexOf
		f = Intrinsic.Create("remove");
		f.AddParam("self");
		f.AddParam("k");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			Value k = context.GetLocal("k");
			if (self instanceof ValMap) {
				ValMap selfMap = (ValMap)self;
				if (k == null) k = ValNull.instance;
				if (selfMap.map.containsKey(k)) {
					selfMap.map.remove(k);
					return Result.True;
				}
				return Result.False;
			} else if (self instanceof ValList) {
				if (k == null) throw new RuntimeException("argument to 'remove' must not be null");
				ValList selfList = (ValList)self;
				int idx = k.IntValue();
				if (idx < 0) idx += selfList.values.size();
				Check.Range(idx, 0, selfList.values.size() - 1);
				selfList.values.remove(idx);
				return Result.Null;
			} else if (self instanceof ValString) {
				if (k == null) throw new RuntimeException("argument to 'remove' must not be null");
				ValString selfStr = (ValString) self;
				String substr = k.toString();
				if (selfStr.value.indexOf(substr) < 0) return new Result(self);
				return new Result(StringUtil.removeFirst(selfStr.value, substr));
			}
			throw new TypeException("Type Error: 'remove' requires map, list, or string");
		};

		// replace
		//	Replace all matching elements of a list or map, or substrings of a string,
		//	with a new value.Lists and maps are mutated in place, and return themselves.
		//	Strings are immutable, so the original string is (of course) unchanged, but
		//	a new string with the replacement is returned.  Note that with maps, it is
		//	the values that are searched for and replaced, not the keys.
		// self (list, map, or string): object to replace elements of
		// oldval (any): value or substring to replace
		// newval (any): new value or substring to substitute where oldval is found
		// maxCount (number, optional): if given, replace no more than this many
		// Returns: modified list or map, or new string, with replacements done
		// Example: "Happy Pappy".replace("app", "ol")		returns "Holy Poly"
		// Example: [1,2,3,2,5].replace(2, 42)		returns (and mutates to) [1, 42, 3, 42, 5]
		// Example: d = {1: "one"}; d.replace("one", "ichi")		returns (and mutates to) {1: "ichi"}
		f = Intrinsic.Create("replace");
		f.AddParam("self");
		f.AddParam("oldval");
		f.AddParam("newval");
		f.AddParam("maxCount");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			if (self == null) throw new RuntimeException("argument to 'replace' must not be null");
			Value oldval = context.GetLocal("oldval");
			Value newval = context.GetLocal("newval");
			Value maxCountVal = context.GetLocal("maxCount");
			int maxCount = -1;
			if (maxCountVal != null) {
				maxCount = maxCountVal.IntValue();
				if (maxCount < 1) return new Result(self);
			}
			int count = 0;
			if (self instanceof ValMap) {
				ValMap selfMap = (ValMap)self;
				// C# doesn't allow changing even the values while iterating
				// over the keys.  So gather the keys to change, then change
				// them afterwards.
				List<Value> keysToChange = null;
				for (Value k : selfMap.map.keySet()) {
					if (selfMap.map.get(k).Equality(oldval) == 1) {
						if (keysToChange == null) keysToChange = new ArrayList<>();
						keysToChange.add(k);
						count++;
						if (maxCount > 0 && count == maxCount) break;
					}
				}
				if (keysToChange != null) for (Value k : keysToChange) {
					selfMap.map.put(k, newval);
				}
				return new Result(self);
			} else if (self instanceof ValList) {
				ValList selfList = (ValList)self;
				int idx = -1;
				while (true) {
					// idx = selfList.values.FindIndex(idx + 1, x -> x.Equality(oldval) == 1);
					boolean found = false;
					for (int i = idx + 1; i < selfList.values.size(); i++) {
						Value x = selfList.values.get(i);
						if (x.Equality(oldval) == 1) {
							idx = i;
							found = true;
							break;
						}
					}
					if (!found) {
						idx = -1;
					}
					if (idx < 0) break;
					selfList.values.set(idx, newval);
					count++;
					if (maxCount > 0 && count == maxCount) break;
				}
				return new Result(self);
			} else if (self instanceof ValString) {
				String str = self.toString();
				String oldstr = oldval == null ? "" : oldval.toString();
				if (oldstr == null || oldstr.isEmpty()) throw new RuntimeException("replace: oldval argument is empty");
				String newstr = newval == null ? "" : newval.toString();
				int idx = 0;
				while (true) {
					idx = str.indexOf(oldstr, idx);
					if (idx < 0) break;
					str = str.substring(0, idx) + newstr + str.substring(idx + oldstr.length());
					idx += newstr.length();
					count++;
					if (maxCount > 0 && count == maxCount) break;
				}
				return new Result(str);
			}
			throw new TypeException("Type Error: 'replace' requires map, list, or string");
		};

		// round
		//	Rounds a number to the specified number of decimal places.  If given
		//	a negative number for decimalPlaces, then rounds to a power of 10:
		//	-1 rounds to the nearest 10, -2 rounds to the nearest 100, etc.
		// x (number): number to round
		// decimalPlaces (number, defaults to 0): how many places past the decimal point to round to
		// Example: round(pi, 2)		returns 3.14
		// Example: round(12345, -3)		returns 12000
		f = Intrinsic.Create("round");
		f.AddParam("x", 0);
		f.AddParam("decimalPlaces", 0);
		f.code = (context, partialResult) -> {
			double num = context.GetLocalDouble("x");
			int decimalPlaces = context.GetLocalInt("decimalPlaces");
			if (decimalPlaces >= 0) {
				if (decimalPlaces > 15) decimalPlaces = 15;
				num = Precision.round(num, decimalPlaces);
			} else {
				double pow10 = Math.pow(10, -decimalPlaces);
				num = Math.round(num / pow10) * pow10;
			}
			return new Result(num);
		};


		// rnd
		//	Generates a pseudorandom number between 0 and 1 (including 0 but
		//	not including 1).  If given a seed, then the generator is reset
		//	with that seed value, allowing you to create repeatable sequences
		//	of random numbers.  If you never specify a seed, then it is
		//	initialized automatically, generating a unique sequence on each run.
		// seed (number, optional): if given, reset the sequence with this value
		// Returns: pseudorandom number in the range [0,1)
		f = Intrinsic.Create("rnd");
		f.AddParam("seed");
		f.code = (context, partialResult) -> {
			if (random == null) random = new Random();
			Value seed = context.GetLocal("seed");
			if (seed != null) random = new Random(seed.IntValue());
			return new Result(random.nextDouble());
		};

		// sign
		//	Return -1 for negative numbers, 1 for positive numbers, and 0 for zero.
		// x (number): number to get the sign of
		// Returns: sign of the number
		// Example: sign(-42.6)		returns -1
		f = Intrinsic.Create("sign");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.signum(context.GetLocalDouble("x")));
		};

		// sin
		//	Returns the sine of the given angle (in radians).
		// radians (number): angle, in radians, to get the sine of
		// Returns: sine of the given angle
		// Example: sin(pi/2)		returns 1
		f = Intrinsic.Create("sin");
		f.AddParam("radians", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.sin(context.GetLocalDouble("radians")));
		};
			
		// slice
		//	Return a subset of a string or list.  This is equivalent to using
		//	the square-brackets slice operator seq[from:to], but with ordinary
		//	function syntax.
		// seq (string or list): sequence to get a subsequence of
		// from (number, default 0): 0-based index to the first element to return (if negative, counts from the end)
		// to (number, optional): 0-based index of first element to *not* include in the result
		//		(if negative, count from the end; if omitted, return the rest of the sequence)
		// Returns: substring or sublist
		// Example: slice("Hello", -2)		returns "lo"
		// Example: slice(["a","b","c","d"], 1, 3)		returns ["b", "c"]
		f = Intrinsic.Create("slice");
		f.AddParam("seq");
		f.AddParam("from", 0);
		f.AddParam("to");
		f.code = (context, partialResult) -> {
			Value seq = context.GetLocal("seq");
			int fromIdx = context.GetLocalInt("from");
			Value toVal = context.GetLocal("to");
			int toIdx = 0;
			if (toVal != null) toIdx = toVal.IntValue();
			if (seq instanceof ValList) {
				List<Value> list = ((ValList)seq).values;
				if (fromIdx < 0) fromIdx += list.size();
				if (fromIdx < 0) fromIdx = 0;
				if (toVal == null) toIdx = list.size();
				if (toIdx < 0) toIdx += list.size();
				if (toIdx > list.size()) toIdx = list.size();
				ValList slice = new ValList();
				if (fromIdx < list.size() && toIdx > fromIdx) {
					for (int i = fromIdx; i < toIdx; i++) {
						slice.values.add(list.get(i));
					}
				}
				return new Result(slice);
			} else if (seq instanceof ValString) {
				String str = ((ValString)seq).value;
				if (fromIdx < 0) fromIdx += str.length();
				if (fromIdx < 0) fromIdx = 0;
				if (toVal == null) toIdx = str.length();
				if (toIdx < 0) toIdx += str.length();
				if (toIdx > str.length()) toIdx = str.length();
				if (toIdx - fromIdx <= 0) return Result.EmptyString;
				return new Result(str.substring(fromIdx, toIdx));
			}
			return Result.Null;
		};
		
		// sort
		//	Sorts a list in place.  With null or no argument, this sorts the
		//	list elements by their own values.  With the byKey argument, each
		//	element is indexed by that argument, and the elements are sorted
		//	by the result.  (This only works if the list elements are maps, or
		//	they are lists and byKey is an integer index.)
		// self (list): list to sort
		// byKey (optional): if given, sort each element by indexing with this key
		// ascending (optional, default true): if false, sort in descending order
		// Returns: self (which has been sorted in place)
		// Example: a = [5,3,4,1,2]; a.sort		results in a == [1, 2, 3, 4, 5]
		// See also: shuffle
		f = Intrinsic.Create("sort");
		f.AddParam("self");
		f.AddParam("byKey");
		f.AddParam("ascending", ValNumber.one);
		f.code = (context, partialResult) -> {
			Value self = context.self;
			ValList list = (ValList) self;
			if (list == null || list.values.size() < 2) return new Result(self);

			Comparator<Value> sorter;
			if (context.GetVar("ascending").BoolValue()) sorter = ValueSorter.instance;
			else sorter = ValueReverseSorter.instance;

			Value byKey = context.GetLocal("byKey");
			if (byKey == null) {
				// Simple case: sort the values as themselves
				list.values.sort(sorter);
			} else {
				// Harder case: sort by a key.
				int count = list.values.size();
				KeyedValue[] arr = new KeyedValue[count];
				for (int i = 0; i < count; i++) {
					arr[i] = new KeyedValue();
					arr[i].value = list.values.get(i);
					//arr[i].valueIndex = i;
				}
				// The key for each item will be the item itself, unless it is a map, in which
				// case it's the item indexed by the given key.  (Works too for lists if our
				// index is an integer.)
				int byKeyInt = byKey.IntValue();
				for (int i = 0; i < count; i++) {
					Value item = list.values.get(i);
					if (item instanceof ValMap) arr[i].sortKey = ((ValMap)item).Lookup(byKey);
					else if (item instanceof ValList) {
						ValList itemList = (ValList)item;
						if (byKeyInt > -itemList.values.size() && byKeyInt < itemList.values.size()) arr[i].sortKey = itemList.values.get(byKeyInt);
						else arr[i].sortKey = null;
					}
				}
				// Now sort our list of keyed values, by key
				// TODO: implement sorting a map by key instead of value
//				KeyedValue[] sortedArr = arr.OrderBy((arg) -> arg.sortKey, sorter);
//				// And finally, convert that back into our list
//				int idx = 0;
//				for (KeyedValue kv : sortedArr) {
//					list.values.add(idx++, kv.value);
//				}
			}
			return new Result(list);
		};

		// split
		//	Split a string into a list, by some delimiter.
		//	May be called with function syntax or dot syntax.
		// self (string): string to split
		// delimiter (string, default " "): substring to split on
		// maxCount (number, default -1): if > 0, split into no more than this many strings
		// Returns: list of substrings found by splitting on delimiter
		// Example: "foo bar baz".split		returns ["foo", "bar", "baz"]
		// Example: "foo bar baz".split("a", 2)		returns ["foo b", "r baz"]
		// See also: join
		f = Intrinsic.Create("split");
		f.AddParam("self");
		f.AddParam("delimiter", " ");
		f.AddParam("maxCount", -1);
		f.code = (context, partialResult) -> {
			String self = context.self.toString();
			String delim = context.GetLocalString("delimiter");
			int maxCount = context.GetLocalInt("maxCount");
			ValList result = new ValList();
			int pos = 0;
			while (pos < self.length()) {
				int nextPos;
				if (maxCount >= 0 && result.values.size() == maxCount - 1) nextPos = self.length();
				else if (delim.length() == 0) nextPos = pos+1;
				else nextPos = self.indexOf(delim, pos);
				if (nextPos < 0) nextPos = self.length();
				result.values.add(new ValString(self.substring(pos, nextPos)));
				pos = nextPos + delim.length();
				if (pos == self.length() && delim.length() > 0) result.values.add(ValString.empty);
			}
			return new Result(result);
		};

		// sqrt
		//	Returns the square root of a number.
		// x (number): number to get the square root of
		// Returns: square root of x
		// Example: sqrt(1764)		returns 42
		f = Intrinsic.Create("sqrt");
		f.AddParam("x", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.sqrt(context.GetLocalDouble("x")));
		};

		// str
		//	Convert any value to a string.
		// x (any): value to convert
		// Returns: string representation of the given value
		// Example: str(42)		returns "42"
		// See also: val
		f = Intrinsic.Create("str");
		f.AddParam("x", ValString.empty);
		f.code = (context, partialResult) -> {		
			Value x = context.GetLocal("x");
			if (x == null) return new Result(ValString.empty);
			return new Result(x.toString());
		};

		// string type
		//	Returns a map that represents the string datatype in
		//	MiniScript's core type system.  This can be used with `isa`
		//	to check whether a variable refers to a string.  You can also
		//	assign new methods here to make them available to all strings.
		// Example: "Hello" isa string		returns 1
		// See also: number, list, map, funcRef
		f = Intrinsic.Create("string");
		f.code = (context, partialResult) -> {
			if (context.vm.stringType == null) {
				context.vm.stringType = StringType().EvalCopy(context.vm.globalContext);
			}
			return new Result(context.vm.stringType);
		};

		// shuffle
		//	Randomize the order of elements in a list, or the mappings from
		//	keys to values in a map.  This is done in place.
		// self (list or map): object to shuffle
		// Returns: null
		f = Intrinsic.Create("shuffle");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			if (random == null) random = new Random();
			if (self instanceof ValList) {
				List<Value> list = ((ValList)self).values;
				// We'll do a Fisher-Yates shuffle, i.e., swap each element
				// with a randomly selected one.
				for (int i=list.size() - 1; i >= 1; i--) {
					int j = random.nextInt(i+1);
					Value temp = list.get(j);
					list.set(j, list.get(i));
					list.set(i, temp);
				}
			} else if (self instanceof ValMap) {
				Map<Value, Value> map = ((ValMap)self).map;
				// Fisher-Yates again, but this time, what we're swapping
				// is the values associated with the keys, not the keys themselves.
				List<Value> keys = new ArrayList<>(map.keySet());
				for (int i = keys.size() - 1; i >= 1; i--) {
					int j = random.nextInt(i+1);
					Value keyi = keys.get(i);
					Value keyj = keys.get(j);
					Value temp = map.get(keyj);
					map.put(keyj, keyi);
					map.put(keyi, temp);
				}
			}
			return Result.Null;
		};

		// sum
		//	Returns the total of all elements in a list, or all values in a map.
		// self (list or map): object to sum
		// Returns: result of adding up all values in self
		// Example: range(3).sum		returns 6 (3 + 2 + 1 + 0)
		f = Intrinsic.Create("sum");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value val = context.self;
			double sum = 0;
			if (val instanceof ValList) {
				List<Value> list = ((ValList)val).values;
				for (Value v : list) {
					sum += v.DoubleValue();
				}
			} else if (val instanceof ValMap) {
				Map<Value, Value> map = ((ValMap)val).map;
				for (Value v : map.values()) {
					sum += v.DoubleValue();
				}
			}
			return new Result(sum);
		};

		// tan
		//	Returns the tangent of the given angle (in radians).
		// radians (number): angle, in radians, to get the tangent of
		// Returns: tangent of the given angle
		// Example: tan(pi/4)		returns 1
		f = Intrinsic.Create("tan");
		f.AddParam("radians", 0);
		f.code = (context, partialResult) -> {
			return new Result(Math.tan(context.GetLocalDouble("radians")));
		};

		// time
		//	Returns the number of seconds since the script started running.
		f = Intrinsic.Create("time");
		f.code = (context, partialResult) -> {
			return new Result(context.vm.runTime());
		};
		
		// upper
		//	Return an upper-case (all capitals) version of a string.
		//	May be called with function syntax or dot syntax.
		// self (string): string to upper-case
		// Returns: string with all lowercase letters converted to capitals
		// Example: "Mo Spam".upper		returns "MO SPAM"
		// See also: lower
		f = Intrinsic.Create("upper");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value val = context.self;
			if (val instanceof ValString) {
				String str = ((ValString)val).value;
				return new Result(str.toUpperCase());
			}
			return new Result(val);
		};
		
		// val
		//	Return the numeric value of a given string.  (If given a number,
		//	returns it as-is; if given a list or map, returns null.)
		//	May be called with function syntax or dot syntax.
		// self (string or number): string to get the value of
		// Returns: numeric value of the given string
		// Example: "1234.56".val		returns 1234.56
		// See also: str
		f = Intrinsic.Create("val");
		f.AddParam("self", 0);
		f.code = (context, partialResult) -> {
			Value val = context.self;
			if (val instanceof ValNumber) return new Result(val);
			if (val instanceof ValString) {
				double value = Double.parseDouble(val.toString());
				return new Result(value);
			}
			return Result.Null;
		};

		// values
		//	Returns the values of a dictionary, or the characters of a string.
		//  (Returns any other value as-is.)
		//	May be called with function syntax or dot syntax.
		// self (any): object to get the values of.
		// Example: d={1:"one", 2:"two"}; d.values		returns ["one", "two"]
		// Example: "abc".values		returns ["a", "b", "c"]
		// See also: indexes
		f = Intrinsic.Create("values");
		f.AddParam("self");
		f.code = (context, partialResult) -> {
			Value self = context.self;
			if (self instanceof ValMap) {
				ValMap map = (ValMap)self;
				List<Value> values = new ArrayList<>(map.map.values());
				return new Result(new ValList(values));
			} else if (self instanceof ValString) {
				String str = ((ValString)self).value;
				List<Value> values = new ArrayList<>(str.length());
				for (int i = 0; i < str.length(); i++) {					
					values.add(TAC.Str(String.valueOf(str.charAt(i))));
				}
				return new Result(new ValList(values));
			}
			return new Result(self);
		};

		// version
		//	Get a map with information about the version of MiniScript and
		//	the host environment that you're currently running.  This will
		//	include at least the following keys:
		//		miniscript: a string such as "1.5"
		//		buildDate: a date in yyyy-mm-dd format, like "2020-05-28"
		//		host: a number for the host major and minor version, like 0.9
		//		hostName: name of the host application, e.g. "Mini Micro"
		//		hostInfo: URL or other short info about the host app
		f = Intrinsic.Create("version");
		f.code = (context, partialResult) -> {
			if (context.vm.versionMap == null) {
				ValMap d = new ValMap();
				d.set("miniscript", new ValString("1.5.1"));
				d.set("buildDate", new ValString("2022-02-06"));
				d.set("host", new ValNumber(HostInfo.version));
				d.set("hostName", new ValString(HostInfo.name));
				d.set("hostInfo", new ValString(HostInfo.info));

				context.vm.versionMap = d;
			}
			return new Result(context.vm.versionMap);
		};

		// wait
		//	Pause execution of this script for some amount of time.
		// seconds (default 1.0): how many seconds to wait
		// Example: wait 2.5		pauses the script for 2.5 seconds
		// See also: time, yield
		f = Intrinsic.Create("wait");
		f.AddParam("seconds", 1);
		f.code = (context, partialResult) -> {
			double now = context.vm.runTime();
			if (partialResult == null) {
				// Just starting our wait; calculate end time and return as partial result
				double interval = context.GetLocalDouble("seconds");
				return new Result(new ValNumber(now + interval), false);
			} else {
				// Continue until current time exceeds the time in the partial result
				if (now > partialResult.result.DoubleValue()) return Result.Null;
				return partialResult;
			}
		};

		// yield
		//	Pause the execution of the script until the next "tick" of
		//	the host app.  In Mini Micro, for example, this waits until
		//	the next 60Hz frame.  Exact meaning may very, but generally
		//	if you're doing something in a tight loop, calling yield is
		//	polite to the host app or other scripts.
		f = Intrinsic.Create("yield");
		f.code = (context, partialResult) -> {
			context.vm.yielding = true;
			return Result.Null;
		};

	}

	static Random random;	// TODO: consider storing this on the context, instead of global!


	// Helper method to compile a call to Slice (when invoked directly via slice syntax).
	public static void CompileSlice(List<Line> code, Value list, Value fromIdx, Value toIdx, int resultTempNum) {
		code.add(new Line(null, Line.Op.PushParam, list));
		code.add(new Line(null, Line.Op.PushParam, fromIdx == null ? TAC.Num(0) : fromIdx));
		code.add(new Line(null, Line.Op.PushParam, toIdx));// toIdx == null ? TAC.Num(0) : toIdx));
		ValFunction func = Intrinsic.GetByName("slice").GetFunc();
		code.add(new Line(TAC.LTemp(resultTempNum), Line.Op.CallFunctionA, func, TAC.Num(3)));
	}
	
	/// <summary>
	/// FunctionType: a static map that represents the Function type.
	/// </summary>
	public static ValMap FunctionType() {
		if (_functionType == null) {
			_functionType = new ValMap();
		}
		return _functionType;
	}
	static ValMap _functionType = null;
	
	/// <summary>
	/// ListType: a static map that represents the List type, and provides
	/// intrinsic methods that can be invoked on it via dot syntax.
	/// </summary>
	public static ValMap ListType() {
		if (_listType == null) {
			_listType = new ValMap();
			_listType.set("hasIndex", Intrinsic.GetByName("hasIndex").GetFunc());
			_listType.set("indexes", Intrinsic.GetByName("indexes").GetFunc());
			_listType.set("indexOf", Intrinsic.GetByName("indexOf").GetFunc());
			_listType.set("insert", Intrinsic.GetByName("insert").GetFunc());
			_listType.set("join", Intrinsic.GetByName("join").GetFunc());
			_listType.set("len", Intrinsic.GetByName("len").GetFunc());
			_listType.set("pop", Intrinsic.GetByName("pop").GetFunc());
			_listType.set("pull", Intrinsic.GetByName("pull").GetFunc());
			_listType.set("push", Intrinsic.GetByName("push").GetFunc());
			_listType.set("shuffle", Intrinsic.GetByName("shuffle").GetFunc());
			_listType.set("sort", Intrinsic.GetByName("sort").GetFunc());
			_listType.set("sum", Intrinsic.GetByName("sum").GetFunc());
			_listType.set("remove", Intrinsic.GetByName("remove").GetFunc());
			_listType.set("replace", Intrinsic.GetByName("replace").GetFunc());
			_listType.set("values", Intrinsic.GetByName("values").GetFunc());
		}
		return _listType;
	}
	static ValMap _listType = null;
	
	/// <summary>
	/// StringType: a static map that represents the String type, and provides
	/// intrinsic methods that can be invoked on it via dot syntax.
	/// </summary>
	public static ValMap StringType() {
		if (_stringType == null) {
			_stringType = new ValMap();
			_stringType.set("hasIndex", Intrinsic.GetByName("hasIndex").GetFunc());
			_stringType.set("indexes", Intrinsic.GetByName("indexes").GetFunc());
			_stringType.set("indexOf", Intrinsic.GetByName("indexOf").GetFunc());
			_stringType.set("insert", Intrinsic.GetByName("insert").GetFunc());
			_stringType.set("code", Intrinsic.GetByName("code").GetFunc());
			_stringType.set("len", Intrinsic.GetByName("len").GetFunc());
			_stringType.set("lower", Intrinsic.GetByName("lower").GetFunc());
			_stringType.set("val", Intrinsic.GetByName("val").GetFunc());
			_stringType.set("remove", Intrinsic.GetByName("remove").GetFunc());
			_stringType.set("replace", Intrinsic.GetByName("replace").GetFunc());
			_stringType.set("split", Intrinsic.GetByName("split").GetFunc());
			_stringType.set("upper", Intrinsic.GetByName("upper").GetFunc());
			_stringType.set("values", Intrinsic.GetByName("values").GetFunc());
		}
		return _stringType;
	}
	static ValMap _stringType = null;
	
	/// <summary>
	/// MapType: a static map that represents the Map type, and provides
	/// intrinsic methods that can be invoked on it via dot syntax.
	/// </summary>
	public static ValMap MapType() {
		if (_mapType == null) {
			_mapType = new ValMap();
			_mapType.set("hasIndex", Intrinsic.GetByName("hasIndex").GetFunc());
			_mapType.set("indexes", Intrinsic.GetByName("indexes").GetFunc());
			_mapType.set("indexOf", Intrinsic.GetByName("indexOf").GetFunc());
			_mapType.set("len", Intrinsic.GetByName("len").GetFunc());
			_mapType.set("pop", Intrinsic.GetByName("pop").GetFunc());
			_mapType.set("push", Intrinsic.GetByName("push").GetFunc());
			_mapType.set("pull", Intrinsic.GetByName("pull").GetFunc());
			_mapType.set("shuffle", Intrinsic.GetByName("shuffle").GetFunc());
			_mapType.set("sum", Intrinsic.GetByName("sum").GetFunc());
			_mapType.set("remove", Intrinsic.GetByName("remove").GetFunc());
			_mapType.set("replace", Intrinsic.GetByName("replace").GetFunc());
			_mapType.set("values", Intrinsic.GetByName("values").GetFunc());
		}
		return _mapType;
	}
	static ValMap _mapType = null;
	
	/// <summary>
	/// NumberType: a static map that represents the Number type.
	/// </summary>
	public static ValMap NumberType() {
		if (_numberType == null) {
			_numberType = new ValMap();
		}
		return _numberType;
	}
	static ValMap _numberType = null;
}
