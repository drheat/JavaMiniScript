package com.catalinionescu.miniscript.intrinsics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Line;
import com.catalinionescu.miniscript.TAC;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.LimitExceededException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.types.Function;
import com.catalinionescu.miniscript.types.Param;
import com.catalinionescu.miniscript.types.ValFunction;
import com.catalinionescu.miniscript.types.ValNumber;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.Value;

/*	MiniscriptIntrinsics.cs
This file defines the Intrinsic class, which represents a built-in function
available to MiniScript code.  All intrinsics are held in static storage, so
this class includes static functions such as GetByName to look up 
already-defined intrinsics.  See Chapter 2 of the MiniScript Integration
Guide for details on adding your own intrinsics.
This file also contains the Intrinsics static class, where all of the standard
intrinsics are defined.  This is initialized automatically, so normally you
don’t need to worry about it, though it is a good place to look for examples
of how to write intrinsic functions.
Note that you should put any intrinsics you add in a separate file; leave the
MiniScript source files untouched, so you can easily replace them when updates
become available.
*/

/// <summary>
/// Intrinsic: represents an intrinsic function available to MiniScript code.
/// </summary>
public class Intrinsic {
	// name of this intrinsic (should be a valid MiniScript identifier)
	public String name;
	
	/// <summary>
	/// IntrinsicCode is a delegate to the actual C# code invoked by an intrinsic method.
	/// </summary>
	/// <param name="context">TAC.Context in which the intrinsic was invoked</param>
	/// <param name="partialResult">partial result from a previous invocation, if any</param>
	/// <returns>result of the computation: whether it's complete, a partial result if not, and a Value if so</returns>
	public interface IntrinsicCode {
		public Result run(Context context, Result partialResult) throws KeyException, TypeException, IndexException, UndefinedIdentifierException, LimitExceededException;
	}
	
	// actual C# code invoked by the intrinsic
	public IntrinsicCode code;
	
	// a numeric ID (used internally -- don't worry about this)
	public int id() {
		return numericID; 
	}

	// static map from Values to short names, used when displaying lists/maps;
	// feel free to add to this any values (especially lists/maps) provided
	// by your own intrinsics.
	public static Map<Value, String> shortNames = new HashMap<>();

	private Function function;
	private ValFunction valFunction;	// (cached wrapper for function)
	int numericID;		// also its index in the 'all' list

	public static List<Intrinsic> all = new ArrayList<Intrinsic>();
	static Map<String, Intrinsic> nameMap = new HashMap<>();
	
	/// <summary>
	/// Factory method to create a new Intrinsic, filling out its name as given,
	/// and other internal properties as needed.  You'll still need to add any
	/// parameters, and define the code it runs.
	/// </summary>
	/// <param name="name">intrinsic name</param>
	/// <returns>freshly minted (but empty) static Intrinsic</returns>
	public static Intrinsic Create(String name) {
		Intrinsic result = new Intrinsic();
		result.name = name;
		result.numericID = all.size();
		result.function = new Function(null);
		result.valFunction = new ValFunction(result.function);
		all.add(result);
		nameMap.put(name, result);
		return result;
	}
	
	/// <summary>
	/// Look up an Intrinsic by its internal numeric ID.
	/// </summary>
	public static Intrinsic GetByID(int id) {
		return all.get(id);
	}
	
	/// <summary>
	/// Look up an Intrinsic by its name.
	/// </summary>
	public static Intrinsic GetByName(String name) {
		Intrinsics.InitIfNeeded();
		
		return nameMap.get(name);
	}
	
	public void AddParam(String name) {
		AddParam(name, (Value) null);
	}
	
	/// <summary>
	/// Add a parameter to this Intrinsic, optionally with a default value
	/// to be used if the user doesn't supply one.  You must add parameters
	/// in the same order in which arguments must be supplied.
	/// </summary>
	/// <param name="name">parameter name</param>
	/// <param name="defaultValue">default value, if any</param>
	public void AddParam(String name, Value defaultValue) {
		function.parameters.add(new Param(name, defaultValue));
	}
	
	/// <summary>
	/// Add a parameter with a numeric default value.  (See comments on
	/// the first version of AddParam above.)
	/// </summary>
	/// <param name="name">parameter name</param>
	/// <param name="defaultValue">default value for this parameter</param>
	public void AddParam(String name, double defaultValue) {
		Value defVal;
		if (defaultValue == 0) defVal = ValNumber.zero;
		else if (defaultValue == 1) defVal = ValNumber.one;
		else defVal = TAC.Num(defaultValue);
		function.parameters.add(new Param(name, defVal));
	}

	/// <summary>
	/// Add a parameter with a string default value.  (See comments on
	/// the first version of AddParam above.)
	/// </summary>
	/// <param name="name">parameter name</param>
	/// <param name="defaultValue">default value for this parameter</param>
	public void AddParam(String name, String defaultValue) {
		Value defVal;
		if (defaultValue == null || defaultValue.isEmpty()) defVal = ValString.empty;
		else if (defaultValue == "__isa") defVal = ValString.magicIsA;
		else if (defaultValue == "self") defVal = _self;
		else defVal = new ValString(defaultValue);
		function.parameters.add(new Param(name, defVal));
	}
	ValString _self = new ValString("self");
	
	/// <summary>
	/// GetFunc is used internally by the compiler to get the MiniScript function
	/// that makes an intrinsic call.
	/// </summary>
	public ValFunction GetFunc() {
		if (function.code == null) {
			// Our little wrapper function is a single opcode: CallIntrinsicA.
			// It really exists only to provide a local variable context for the parameters.
			function.code = new ArrayList<Line>();
			function.code.add(new Line(TAC.LTemp(0), Line.Op.CallIntrinsicA, TAC.Num(numericID)));
		}
		return valFunction;
	}
	
	/// <summary>
	/// Internally-used function to execute an intrinsic (by ID) given a
	/// context and a partial result.
	/// </summary>
	public static Result Execute(int id, Context context, Result partialResult) throws KeyException, TypeException, IndexException, UndefinedIdentifierException, LimitExceededException {
		Intrinsic item = GetByID(id);
		return item.code.run(context, partialResult);
	}
}
