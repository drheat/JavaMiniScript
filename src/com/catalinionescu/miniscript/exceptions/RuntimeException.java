package com.catalinionescu.miniscript.exceptions;

public class RuntimeException extends MiniscriptException {
	private static final long serialVersionUID = 1L;

	public RuntimeException() {
		super("Runtime Error");
	}
	
	public RuntimeException(String text) {
		super(text);
	}
	
	public RuntimeException(String message, Exception inner) {
		super(message, inner);
	}
}
