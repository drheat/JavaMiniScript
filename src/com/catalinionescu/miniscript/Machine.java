package com.catalinionescu.miniscript;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Map.Entry;

import com.catalinionescu.miniscript.Interpreter.TextOutputMethod;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.LimitExceededException;
import com.catalinionescu.miniscript.exceptions.MiniscriptException;
import com.catalinionescu.miniscript.exceptions.SourceLoc;
import com.catalinionescu.miniscript.exceptions.TooManyArgumentsException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.intrinsics.Intrinsic;
import com.catalinionescu.miniscript.types.Pair;
import com.catalinionescu.miniscript.types.ValFunction;
import com.catalinionescu.miniscript.types.ValMap;
import com.catalinionescu.miniscript.types.ValSeqElem;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.ValVar;
import com.catalinionescu.miniscript.types.Value;
import com.catalinionescu.miniscript.utils.Stopwatch;

/// <summary>
/// TAC.Machine implements a complete MiniScript virtual machine.  It 
/// keeps the context stack, keeps track of run time, and provides 
/// methods to step, stop, or reset the program.		
/// </summary>
public class Machine {
	public WeakReference<Interpreter> interpreter;		// interpreter hosting this machine
	public TextOutputMethod standardOutput;	// where print() results should go
	public boolean storeImplicit = false;		// whether to store implicit values (e.g. for REPL)
	public boolean yielding = false;			// set to true by yield intrinsic
	public ValMap functionType;
	public ValMap listType;
	public ValMap mapType;
	public ValMap numberType;
	public ValMap stringType;
	public ValMap versionMap;
	
	public Context globalContext;				// contains global variables

	public boolean done() {
		return (stack.size() <= 1 && stack.peek().done());
	}

	public long runTime() {
		return stopwatch == null ? 0 : stopwatch.getElapsedSeconds();
	}
	
	Stack<Context> stack;
	Stopwatch stopwatch;

	public Machine(Context globalContext, TextOutputMethod standardOutput) {
		this.globalContext = globalContext;
		this.globalContext.vm = this;
		if (standardOutput == null) {
			this.standardOutput = s -> System.out.println(s);
		} else {
			this.standardOutput = standardOutput;
		}
		stack = new Stack<Context>();
		stack.push(this.globalContext);
	}

	public void Stop() {
		while (stack.size() > 1) stack.pop();
		stack.peek().JumpToEnd();
	}
	
	public void Reset() {
		while (stack.size() > 1) stack.pop();
		stack.peek().Reset(false);
	}

	public void Step() throws MiniscriptException {
		if (stack.size() == 0) return;		// not even a global context
		if (stopwatch == null) {
			stopwatch = new Stopwatch();
			stopwatch.start();
		}
		Context context = stack.peek();
		while (context.done()) {
			if (stack.size() == 1) return;	// all done (can't pop the global context)
			PopContext();
			context = stack.peek();
		}

		Line line = context.code.get(context.lineNum++);
		try {
			DoOneLine(line, context);
		} catch (MiniscriptException mse) {
			if (mse.location == null) mse.location = line.location;
			if (mse.location == null) {
				for (Context c : stack) {
					if (c.lineNum >= c.code.size()) continue;
					mse.location = c.code.get(c.lineNum).location;
					if (mse.location != null) break;
				}
			}
			throw mse;
		}
	}
	
	public void ManuallyPushCall(ValFunction func) throws TooManyArgumentsException {
		ManuallyPushCall(func, null);
	}
	
	/// <summary>
	/// Directly invoke a ValFunction by manually pushing it onto the call stack.
	/// This might be useful, for example, in invoking handlers that have somehow
	/// been registered with the host app via intrinsics.
	/// </summary>
	/// <param name="func">Miniscript function to invoke</param>
	/// <param name="resultStorage">where to store result of the call, in the calling context</param>
	public void ManuallyPushCall(ValFunction func, Value resultStorage) throws TooManyArgumentsException {
		int argCount = 0;
		Value self = null;	// "self" is always null for a manually pushed call
		Context nextContext = stack.peek().NextCallContext(func.function, argCount, self != null, null);
		if (self != null) nextContext.self = self;		// TODO: dead code
		nextContext.resultStorage = resultStorage;
		stack.push(nextContext);				
	}
	
	void DoOneLine(Line line, Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException, TooManyArgumentsException, LimitExceededException {
//		Console.WriteLine("EXECUTING line " + (context.lineNum-1) + ": " + line);
		if (line.op == Line.Op.PushParam) {
			Value val = context.ValueInContext(line.rhsA);
			context.PushParamArgument(val);
		} else if (line.op == Line.Op.CallFunctionA) {
			// Resolve rhsA.  If it's a function, invoke it; otherwise,
			// just store it directly (but pop the call context).
			Pair<Value, ValMap> result = line.rhsA.ValPair(context);	// resolves the whole dot chain, if any
			// TODO: make sure infinite recursive calls are properly handled, see TestSuite @ line 819
			if (result != null && result.getFirst() instanceof ValFunction) {
				Value self = null;
				// bind "super" to the parent of the map the function was found in
				// TODO: check out that super here works as superV
				Value superV = result.getSecond() == null ? null : result.getSecond().Lookup(ValString.magicIsA);
				if (line.rhsA instanceof ValSeqElem) {
					// bind "self" to the object used to invoke the call, except
					// when invoking via "super"
					Value seq = ((ValSeqElem)(line.rhsA)).sequence;
					if (seq instanceof ValVar && ((ValVar)seq).identifier.equals("super")) self = context.self;
					else self = context.ValueInContext(seq);
				}
				ValFunction func = (ValFunction) result.getFirst();
				int argCount = line.rhsB.IntValue();
				Context nextContext = context.NextCallContext(func.function, argCount, self != null, line.lhs);
				nextContext.outerVars = func.outerVars;
				if (result.getSecond() != null) nextContext.SetVar("super", superV);
				if (self != null) nextContext.self = self;	// (set only if bound above)
				stack.push(nextContext);
			} else if (result != null) {
				// The user is attempting to call something that's not a function.
				// We'll allow that, but any number of parameters is too many.  [#35]
				// (No need to pop them, as the exception will pop the whole call stack anyway.)
				int argCount = line.rhsB.IntValue();
				if (argCount > 0) throw new TooManyArgumentsException();
				context.StoreValue(line.lhs, result.getFirst());
			}
		} else if (line.op == Line.Op.ReturnA) {
			Value val = line.Evaluate(context);
			context.StoreValue(line.lhs, val);
			PopContext();
		} else if (line.op == Line.Op.AssignImplicit) {
			Value val = line.Evaluate(context);
			if (storeImplicit) {
				context.StoreValue(ValVar.implicitResult, val);
				context.implicitResultCounter++;
			}
		} else {
			Value val = line.Evaluate(context);
			context.StoreValue(line.lhs, val);
		}
	}

	void PopContext() throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// Our top context is done; pop it off, and copy the return value in temp 0.
		if (stack.size() == 1) return;	// down to just the global stack (which we keep)
		Context context = stack.pop();
		Value result = context.GetTemp(0, null);
		Value storage = context.resultStorage;
		context = stack.peek();
		context.StoreValue(storage, result);
	}

	public Context GetTopContext() {
		return stack.peek();
	}

	public void DumpTopContext() {
		stack.peek().Dump();
	}
	
	public String FindShortName(Value val) {
		if (globalContext == null || globalContext.variables == null) return null;
		for (Entry<Value, Value> kv : globalContext.variables.map.entrySet()) {
			if (kv.getValue() == val && kv.getKey() != val) return kv.getKey().toString(this);
		}

		return Intrinsic.shortNames.get(val);
	}
	
	public List<SourceLoc> GetStack() {
		List<SourceLoc> result = new ArrayList<>();
		for (Context context : stack) {
			result.add(context.GetSourceLoc());
		}
		return result;
	}
}