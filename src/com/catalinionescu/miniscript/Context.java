package com.catalinionescu.miniscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.SourceLoc;
import com.catalinionescu.miniscript.exceptions.TooManyArgumentsException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.intrinsics.Intrinsic;
import com.catalinionescu.miniscript.intrinsics.Result;
import com.catalinionescu.miniscript.types.Function;
import com.catalinionescu.miniscript.types.Pair;
import com.catalinionescu.miniscript.types.ValMap;
import com.catalinionescu.miniscript.types.ValSeqElem;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.ValTemp;
import com.catalinionescu.miniscript.types.ValVar;
import com.catalinionescu.miniscript.types.Value;

/// <summary>
/// TAC.Context keeps track of the runtime environment, including local 
/// variables.  Context objects form a linked list via a "parent" reference,
/// with a new context formed on each function call (this is known as the
/// call stack).
/// </summary>
public class Context {
	public List<Line> code;			// TAC lines we're executing
	public int lineNum;				// next line to be executed
	public ValMap variables;		// local variables for this call frame
	public ValMap outerVars;        // variables of the context where this function was defined
	public Value self;				// value of self in this context
	public Stack<Value> args;		// pushed arguments for upcoming calls
	public Context parent;			// parent (calling) context
	public Value resultStorage;		// where to store the return value (in the calling context)
	public Machine vm;				// virtual machine
	public Result partialResult;	// work-in-progress of our current intrinsic
	public int implicitResultCounter;	// how many times we have stored an implicit result

	public boolean done() {
		return lineNum >= code.size();
	}

	public Context root() {
		Context c = this;
		while (c.parent != null) c = c.parent;
		return c;
	}

	public Interpreter interpreter() {
		if (vm == null || vm.interpreter == null) return null;
		return (Interpreter) vm.interpreter.get();
	}

	List<Value> temps;			// values of temporaries; temps[0] is always return value

	public Context(List<Line> code) {
		this.code = code;
	}

	public void ClearCodeAndTemps() {
 		code.clear();
		lineNum = 0;
		if (temps != null) temps.clear();
	}
	
	public void Reset() {
		Reset(true);
	}

	/// <summary>
	/// Reset this context to the first line of code, clearing out any 
	/// temporary variables, and optionally clearing out all variables.
	/// </summary>
	/// <param name="clearVariables">if true, clear our local variables</param>
	public void Reset(boolean clearVariables) {
		lineNum = 0;
		temps = null;
		if (clearVariables) variables = new ValMap();
	}

	public void JumpToEnd() {
		lineNum = code.size();
	}

	public void SetTemp(int tempNum, Value value) {
		// OFI: let each context record how many temps it will need, so we
		// can pre-allocate this list with that many and avoid having to
		// grow it later.  Also OFI: do lifetime analysis on these temps
		// and reuse ones we don't need anymore.
		if (temps == null) temps = new ArrayList<>();
		while (temps.size() <= tempNum) temps.add(null);
		temps.set(tempNum, value);
	}

	public Value GetTemp(int tempNum) {
		return temps == null ? null : temps.get(tempNum);
	}

	public Value GetTemp(int tempNum, Value defaultValue) {
		if (temps != null && tempNum < temps.size()) return temps.get(tempNum);
		return defaultValue;
	}

	public void SetVar(String identifier, Value value) {
		if (identifier.equals("globals") || identifier.equals("locals")) {
			throw new RuntimeException("can't assign to " + identifier);
		}
		if (identifier.equals("self")) self = value;
		if (variables == null) variables = new ValMap();
		if (variables.assignOverride == null || !variables.assignOverride.run(new ValString(identifier), value)) {
			variables.set(identifier, value);
		}
	}
	
	public Value GetLocal(String identifier) {
		return GetLocal(identifier, null);
	}
	
	/// <summary>
	/// Get the value of a local variable ONLY -- does not check any other
	/// scopes, nor check for special built-in identifiers like "globals".
	/// Used mainly by host apps to easily look up an argument to an
	/// intrinsic function call by the parameter name.
	/// </summary>
	public Value GetLocal(String identifier, Value defaultValue) {
		if (variables != null && variables.ContainsKey(identifier)) {
			return variables.get(identifier);
		}
		return defaultValue;
	}
	
	public int GetLocalInt(String identifier) {
		return GetLocalInt(identifier, 0);
	}
	
	public int GetLocalInt(String identifier, int defaultValue) {
		if (variables == null) {
			return defaultValue;
		}
		
		Pair<Boolean, Value> result = variables.TryGetValue(identifier);				
		if (result.getFirst()) {
			if (result.getSecond() == null) return 0;	// variable found, but its value was null!
			return result.getSecond().IntValue();
		}
		return defaultValue;
	}
	
	public boolean GetLocalBool(String identifier) {
		return GetLocalBool(identifier, false);
	}

	public boolean GetLocalBool(String identifier, boolean defaultValue) {
		if (variables == null) {
			return defaultValue;
		}
		
		Pair<Boolean, Value> result = variables.TryGetValue(identifier);
		if (result.getFirst()) {
			if (result.getSecond() == null) return false;	// variable found, but its value was null!
			return result.getSecond().BoolValue();
		}
		return defaultValue;
	}
	
	public float GetLocalFloat(String identifier) {
		return GetLocalFloat(identifier, 0);
	}

	public float GetLocalFloat(String identifier, float defaultValue) {
		if (variables == null) {
			return defaultValue;
		}
		
		Pair<Boolean, Value> result = variables.TryGetValue(identifier);
		if (result.getFirst()) {
			if (result.getSecond() == null) return 0;	// variable found, but its value was null!
			return result.getSecond().FloatValue();
		}
		return defaultValue;
	}
	
	public double GetLocalDouble(String identifier) {
		return GetLocalDouble(identifier, 0);
	}

	public double GetLocalDouble(String identifier, double defaultValue) {
		if (variables == null) {
			return defaultValue;
		}
		
		Pair<Boolean, Value> result = variables.TryGetValue(identifier);
		
		if (result.getFirst()) {
			if (result.getSecond() == null) return 0;	// variable found, but its value was null!
			return result.getSecond().DoubleValue();
		}
		return defaultValue;
	}
	
	public String GetLocalString(String identifier) {
		return GetLocalString(identifier, null);
	}

	public String GetLocalString(String identifier, String defaultValue) {
		if (variables == null) {
			return defaultValue;
		}
		
		Pair<Boolean, Value> result = variables.TryGetValue(identifier);
		
		if (result.getFirst()) {
			if (result.getSecond() == null) return null;	// variable found, but its value was null!
			return result.getSecond().toString();
		}
		return defaultValue;
	}

	public SourceLoc GetSourceLoc() {
		if (lineNum < 0 || lineNum >= code.size()) return null;
		return code.get(lineNum).location;
	}
	
	/// <summary>
	/// Get the value of a variable available in this context (including
	/// locals, globals, and intrinsics).  Raise an exception if no such
	/// identifier can be found.
	/// </summary>
	/// <param name="identifier">name of identifier to look up</param>
	/// <returns>value of that identifier</returns>
	public Value GetVar(String identifier) throws UndefinedIdentifierException {
//		try {
//			throw new Exception();
//		} catch (Exception e) {
//			System.out.println("GetVar: " + identifier);
//			e.printStackTrace();
//		}
		
		// check for special built-in identifiers 'locals', 'globals', etc.
		if (identifier.equals("self")) return self;
		if (identifier.equals("locals")) {
			if (variables == null) variables = new ValMap();
			return variables;
		}
		if (identifier.equals("globals")) {
			if (root().variables == null) root().variables = new ValMap();
			return root().variables;
		}
		if (identifier.equals("outer")) {
			// return module variables, if we have them; else globals
			if (outerVars != null) return outerVars;
			if (root().variables == null) root().variables = new ValMap();
			return root().variables;
		}
		
		// check for a local variable
		if (variables != null) {
			Pair<Boolean, Value> result = variables.TryGetValue(identifier);
			if (result.getFirst()) {
				return result.getSecond();
			}
		}

		// check for a module variable
		if (outerVars != null) {
			Pair<Boolean, Value> result = outerVars.TryGetValue(identifier);
			if (result.getFirst()) {
				return result.getSecond();
			}
		}

		// OK, we don't have a local or module variable with that name.
		// Check the global scope (if that's not us already).
		if (parent != null) {
			Context globals = root();
			if (globals.variables != null) {
				Pair<Boolean, Value> result = globals.variables.TryGetValue(identifier);
				if (result.getFirst()) {
					return result.getSecond();
				}
			}
		}

		// Finally, check intrinsics.
		Intrinsic intrinsic = Intrinsic.GetByName(identifier);
		if (intrinsic != null) return intrinsic.GetFunc();

		// No luck there either?  Undefined identifier.
		throw new UndefinedIdentifierException(identifier);
	}

	public void StoreValue(Value lhs, Value value) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		if (lhs instanceof ValTemp) {
			SetTemp(((ValTemp)lhs).tempNum, value);
		} else if (lhs instanceof ValVar) {
			SetVar(((ValVar)lhs).identifier, value);
		} else if (lhs instanceof ValSeqElem) {
			ValSeqElem seqElem = (ValSeqElem)lhs;
			Value seq = seqElem.sequence.Val(this);
			if (seq == null) throw new RuntimeException("can't set indexed element of null");
			if (!seq.CanSetElem()) {
				throw new RuntimeException("can't set an indexed element in this type");
			}
			Value index = seqElem.index;
			if (index instanceof ValVar || index instanceof ValSeqElem || 
				index instanceof ValTemp) index = index.Val(this);
			seq.SetElem(index, value);
		} else {
			if (lhs != null) throw new RuntimeException("not an lvalue");
		}
	}
	
	public Value ValueInContext(Value value) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		if (value == null) return null;
		return value.Val(this);
	}

	/// <summary>
	/// Store a parameter argument in preparation for an upcoming call
	/// (which should be executed in the context returned by NextCallContext).
	/// </summary>
	/// <param name="arg">Argument.</param>
	public void PushParamArgument(Value arg) {
		if (args == null) args = new Stack<Value>();
		if (args.size() > 255) throw new RuntimeException("Argument limit exceeded");
		args.push(arg);				
	}

	/// <summary>
	/// Get a context for the next call, which includes any parameter arguments
	/// that have been set.
	/// </summary>
	/// <returns>The call context.</returns>
	/// <param name="func">Function to call.</param>
	/// <param name="argCount">How many arguments to pop off the stack.</param>
	/// <param name="gotSelf">Whether this method was called with dot syntax.</param> 
	/// <param name="resultStorage">Value to stuff the result into when done.</param>
	public Context NextCallContext(Function func, int argCount, boolean gotSelf, Value resultStorage) throws TooManyArgumentsException {
		Context result = new Context(func.code);

		result.code = func.code;
		result.resultStorage = resultStorage;
		result.parent = this;
		result.vm = vm;

		// Stuff arguments, stored in our 'args' stack,
		// into local variables corrersponding to parameter names.
		// As a special case, skip over the first parameter if it is named 'self'
		// and we were invoked with dot syntax.
		int selfParam = (gotSelf && func.parameters.size() > 0 && func.parameters.get(0).name.equals("self") ? 1 : 0);
		for (int i = 0; i < argCount; i++) {
			// Careful -- when we pop them off, they're in reverse order.
			Value argument = args.pop();
			int paramNum = argCount - 1 - i + selfParam;
			if (paramNum >= func.parameters.size()) {
				throw new TooManyArgumentsException();
			}
			String param = func.parameters.get(paramNum).name;
			if (param.equals("self")) result.self = argument;
			else result.SetVar(param, argument);
		}
		// And fill in the rest with default values
		for (int paramNum = argCount+selfParam; paramNum < func.parameters.size(); paramNum++) {
			result.SetVar(func.parameters.get(paramNum).name, func.parameters.get(paramNum).defaultValue);
		}

		return result;
	}

	/// <summary>
	/// This function prints the three-address code to the console, for debugging purposes.
	/// </summary>
	public void Dump() {
		System.out.println("CODE:");
		TAC.Dump(code, lineNum);

		System.out.println("\nVARS:");
		if (variables == null) {
			System.out.println(" NONE");
		} else {
			for (Value v : variables.Keys()) {
				String id = v.toString(vm);
				System.out.println(String.format("%s: %s", id, variables.get(id).toString(vm)));
			}
		}

		System.out.println("\nTEMPS:");
		if (temps == null) {
			System.out.println(" NONE");
		} else {
			for (int i = 0; i < temps.size(); i++) {
				System.out.println(String.format("_{0}: {1}", i, temps.get(i)));
			}
		}
	}

	@Override
	public String toString() {
		return String.format("Context[%d/%d]", lineNum, code.size());
	}
}
