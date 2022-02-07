package com.catalinionescu.miniscript.exceptions;

public class CompilerException extends MiniscriptException {
	private static final long serialVersionUID = 1L;

	public CompilerException() {
		super("Syntax Error");
	}
	
	public CompilerException(String text) {
		super(text);
	}
	
	public CompilerException(String context, int lineNum, String message) {
		super(context, lineNum, message);
	}
	
	public CompilerException(String message, Exception inner) {
		super(message, inner);
	}
}
