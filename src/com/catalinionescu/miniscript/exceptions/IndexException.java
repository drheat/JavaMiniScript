package com.catalinionescu.miniscript.exceptions;

public class IndexException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public IndexException() { 
		super("Index Error (index out of range)");
	}
	
	public IndexException(String text) {
		super(text);
	}
	
	public IndexException(String message, Exception inner) {
		super(message, inner);
	}
}
