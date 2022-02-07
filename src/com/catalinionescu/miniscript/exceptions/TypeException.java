package com.catalinionescu.miniscript.exceptions;

public class TypeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TypeException() {
		super("Type Error (wrong type for whatever you're doing)");
	}
	
	public TypeException(String text) {
		super(text);
	}
	
	public TypeException(String message, Exception inner) {
		super(message, inner);
	}
}
