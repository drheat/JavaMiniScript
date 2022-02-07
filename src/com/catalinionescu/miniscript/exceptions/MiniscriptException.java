package com.catalinionescu.miniscript.exceptions;

public class MiniscriptException extends Exception {
	private static final long serialVersionUID = 1L;
	public SourceLoc location;

	public MiniscriptException() {
		super();
	}

	public MiniscriptException(String message) {
		super(message);
	}

	public MiniscriptException(String context, int lineNum, String message) {
		super(message);
		location = new SourceLoc(context, lineNum);
	}

	public MiniscriptException(String message, Exception inner) {
		super(message, inner);
	}

	/// <summary>
	/// Get a standard description of this error, including type and location.
	/// </summary>
	public String Description() {
		String desc = "Error: ";
		if (this instanceof LexerException) {
			desc = "Lexer Error: ";
		} else if (this instanceof CompilerException) {
			desc = "Compiler Error: ";
		} else if (this instanceof RuntimeException) {
			desc = "Runtime Error: ";
		}
		desc += getMessage();
		if (location != null) desc += " " + location;
		return desc;		
	}
}
