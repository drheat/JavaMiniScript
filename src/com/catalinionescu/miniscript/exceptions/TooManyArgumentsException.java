package com.catalinionescu.miniscript.exceptions;

public class TooManyArgumentsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public TooManyArgumentsException() {
		super("Too Many Arguments");
	}
	
	public TooManyArgumentsException(String text) {
		super(text);
	}
	
	public TooManyArgumentsException(String message, Exception inner) {
		super(message, inner);
	}
}
