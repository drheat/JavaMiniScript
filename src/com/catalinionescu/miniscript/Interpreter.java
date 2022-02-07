package com.catalinionescu.miniscript;

import java.lang.ref.WeakReference;
import java.util.List;

import com.catalinionescu.miniscript.exceptions.MiniscriptException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.types.ValVar;
import com.catalinionescu.miniscript.types.Value;

public class Interpreter {
	/// <summary>
	/// TextOutputMethod: a delegate used to return text from the script
	/// (e.g. normal output, errors, etc.) to your C# code.
	/// </summary>
	/// <param name="output"></param>
	public interface TextOutputMethod {
		public void print(String output);
	}
	
	/// <summary>
	/// standardOutput: receives the output of the "print" intrinsic.
	/// </summary>	
	public TextOutputMethod getStandardOutput() {
		return _standardOutput;
	}
	
	public void setStandardOutput(TextOutputMethod value) {
		_standardOutput = value;
		if (vm != null) vm.standardOutput = value;
		
	}
	
	/// <summary>
	/// implicitOutput: receives the value of expressions entered when
	/// in REPL mode.  If you're not using the REPL() method, you can
	/// safely ignore this.
	/// </summary>
	public TextOutputMethod implicitOutput;
	
	/// <summary>
	/// errorOutput: receives error messages from the runtime.  (This happens
	/// via the ReportError method, which is virtual; so if you want to catch
	/// the actual exceptions rather than get the error messages as strings,
	/// you can subclass Interpreter and override that method.)
	/// </summary>
	public TextOutputMethod errorOutput;
	
	/// <summary>
	/// hostData is just a convenient place for you to attach some arbitrary
	/// data to the interpreter.  It gets passed through to the context object,
	/// so you can access it inside your custom intrinsic functions.  Use it
	/// for whatever you like (or don't, if you don't feel the need).
	/// </summary>
	public Object hostData;
	
	/// <summary>
	/// done: returns true when we don't have a virtual machine, or we do have
	/// one and it is done (has reached the end of its code).
	/// </summary>
	public boolean done() {
		return vm == null || vm.done();	
	}
	
	/// <summary>
	/// vm: the virtual machine this interpreter is running.  Most applications will
	/// not need to use this, but it's provided for advanced users.
	/// </summary>
	public Machine vm;
	
	TextOutputMethod _standardOutput;
	String source;
	Parser parser;
	
	public Interpreter() {
		this(null, null, null);
	}
	
	public Interpreter(String source) {
		this(source, null, null);
	}
	
	public Interpreter(String source, TextOutputMethod standardOutput) {
		this(source, standardOutput, null);
	}
	
	/// <summary>
	/// Constructor taking some MiniScript source code, and the output delegates.
	/// </summary>
	public Interpreter(String source, TextOutputMethod standardOutput, TextOutputMethod errorOutput) {
		this.source = source;
		if (standardOutput == null) standardOutput = s -> System.out.println(s);
		if (errorOutput == null) errorOutput = s -> System.err.println(s);
		setStandardOutput(standardOutput);
		this.errorOutput = errorOutput;
	}
	
	/// <summary>
	/// Constructor taking source code in the form of a list of strings.
	/// </summary>
	public Interpreter(List<String> source) {
		this(String.join("\n", source));
	}
	
	/// <summary>
	/// Constructor taking source code in the form of a string array.
	/// </summary>
	public Interpreter(String[] source) {
		this(String.join("\n", source));
	}
	
	/// <summary>
	/// Stop the virtual machine, and jump to the end of the program code.
	/// Also reset the parser, in case it's stuck waiting for a block ender.
	/// </summary>
	public void Stop() {
		if (vm != null) vm.Stop();
		if (parser != null) parser.PartialReset();
	}
	
	public void Reset() {
		Reset("");
	}
	
	/// <summary>
	/// Reset the interpreter with the given source code.
	/// </summary>
	/// <param name="source"></param>
	public void Reset(String source) {
		this.source = source;
		parser = null;
		vm = null;
	}
	
	/// <summary>
	/// Compile our source code, if we haven't already done so, so that we are
	/// either ready to run, or generate compiler errors (reported via errorOutput).
	/// </summary>
	public void Compile() {
		if (vm != null) return;	// already compiled

		if (parser == null) parser = new Parser();
		try {
			parser.Parse(source);
			vm = parser.CreateVM(getStandardOutput());
			vm.interpreter = new WeakReference<>(this);
		} catch (MiniscriptException mse) {
			ReportError(mse);
		}
	}
	
	/// <summary>
	/// Reset the virtual machine to the beginning of the code.  Note that this
	/// does *not* reset global variables; it simply clears the stack and jumps
	/// to the beginning.  Useful in cases where you have a short script you
	/// want to run over and over, without recompiling every time.
	/// </summary>
	public void Restart() {
		if (vm != null) vm.Reset();			
	}
	
	public void RunUntilDone() {
		RunUntilDone(60, true);
	}
	
	public void RunUntilDone(double timeLimit) {
		RunUntilDone(timeLimit, true);
	}
	
	/// <summary>
	/// Run the compiled code until we either reach the end, or we reach the
	/// specified time limit.  In the latter case, you can then call RunUntilDone
	/// again to continue execution right from where it left off.
	/// 
	/// Or, if returnEarly is true, we will also return if we reach an intrinsic
	/// method that returns a partial result, indicating that it needs to wait
	/// for something.  Again, call RunUntilDone again later to continue.
	/// 
	/// Note that this method first compiles the source code if it wasn't compiled
	/// already, and in that case, may generate compiler errors.  And of course
	/// it may generate runtime errors while running.  In either case, these are
	/// reported via errorOutput.
	/// </summary>
	/// <param name="timeLimit">maximum amout of time to run before returning, in seconds</param>
	/// <param name="returnEarly">if true, return as soon as we reach an intrinsic that returns a partial result</param>
	public void RunUntilDone(double timeLimit, boolean returnEarly) {
		try {
			if (vm == null) {
				Compile();
				if (vm == null) return;	// (must have been some error)
			}
			double startTime = vm.runTime();
			vm.yielding = false;
			while (!vm.done() && !vm.yielding) {
				if (vm.runTime() - startTime > timeLimit) return;	// time's up for now!
				vm.Step();		// update the machine
				if (returnEarly && vm.GetTopContext().partialResult != null) return;	// waiting for something
			}
		} catch (MiniscriptException mse) {
			ReportError(mse);
			Stop(); // was: vm.GetTopContext().JumpToEnd();
		}
	}
	
	/// <summary>
	/// Run one step of the virtual machine.  This method is not very useful
	/// except in special cases; usually you will use RunUntilDone (above) instead.
	/// </summary>
	public void Step() {
		try {
			Compile();
			vm.Step();
		} catch (MiniscriptException mse) {
			ReportError(mse);
			Stop(); // was: vm.GetTopContext().JumpToEnd();
		}
	}

	public void REPL(String sourceLine) {
		REPL(sourceLine, 60);
	}
	
	/// <summary>
	/// Read Eval Print Loop.  Run the given source until it either terminates,
	/// or hits the given time limit.  When it terminates, if we have new
	/// implicit output, print that to the implicitOutput stream.
	/// </summary>
	/// <param name="sourceLine">Source line.</param>
	/// <param name="timeLimit">Time limit.</param>
	public void REPL(String sourceLine, double timeLimit) {
		if (parser == null) parser = new Parser();
		if (vm == null) {
			vm = parser.CreateVM(getStandardOutput());
			vm.interpreter = new WeakReference<>(this);
		} else if (vm.done() && !parser.NeedMoreInput()) {
			// Since the machine and parser are both done, we don't really need the
			// previously-compiled code.  So let's clear it out, as a memory optimization.
			vm.GetTopContext().ClearCodeAndTemps();
			parser.PartialReset();
        }
		if (sourceLine == "#DUMP") {
			vm.DumpTopContext();
			return;
		}
		
		double startTime = vm.runTime();
		int startImpResultCount = vm.globalContext.implicitResultCounter;
		vm.storeImplicit = (implicitOutput != null);

		try {
			if (sourceLine != null) parser.Parse(sourceLine, true);
			if (!parser.NeedMoreInput()) {
				while (!vm.done() && !vm.yielding) {
					if (vm.runTime() - startTime > timeLimit) return;	// time's up for now!
					vm.Step();
				}
				if (implicitOutput != null && vm.globalContext.implicitResultCounter > startImpResultCount) {

					Value result = vm.globalContext.GetVar(ValVar.implicitResult.identifier);
					if (result != null) {
						implicitOutput.print(result.ToString(vm));
					}
				}
			}

		} catch (MiniscriptException mse) {
			ReportError(mse);
			// Attempt to recover from an error by jumping to the end of the code.
			Stop(); // was: vm.GetTopContext().JumpToEnd();
		}
	}
	
	/// <summary>
	/// Report whether the virtual machine is still running, that is,
	/// whether it has not yet reached the end of the program code.
	/// </summary>
	/// <returns></returns>
	public boolean Running() {
		return vm != null && !vm.done();
	}
	
	/// <summary>
	/// Return whether the parser needs more input, for example because we have
	/// run out of source code in the middle of an "if" block.  This is typically
	/// used with REPL for making an interactive console, so you can change the
	/// prompt when more input is expected.
	/// </summary>
	/// <returns></returns>
	public boolean NeedMoreInput() {
		return parser != null && parser.NeedMoreInput();
	}
	
	/// <summary>
	/// Get a value from the global namespace of this interpreter.
	/// </summary>
	/// <param name="varName">name of global variable to get</param>
	/// <returns>Value of the named variable, or null if not found</returns>
	public Value GetGlobalValue(String varName) {
		if (vm == null) return null;
		Context c = vm.globalContext;
		if (c == null) return null;
		try {
			return c.GetVar(varName);
		} catch (UndefinedIdentifierException e) {
			return null;
		}
	}
	
	/// <summary>
	/// Set a value in the global namespace of this interpreter.
	/// </summary>
	/// <param name="varName">name of global variable to set</param>
	/// <param name="value">value to set</param>
	public void SetGlobalValue(String varName, Value value) {
		if (vm != null) vm.globalContext.SetVar(varName, value);
	}
	
	/// <summary>
	/// Report a MiniScript error to the user.  The default implementation 
	/// simply invokes errorOutput with the error description.  If you want
	/// to do something different, then make an Interpreter subclass, and
	/// override this method.
	/// </summary>
	/// <param name="mse">exception that was thrown</param>
	protected void ReportError(MiniscriptException mse) {
		errorOutput.print(mse.Description());
	}
}
