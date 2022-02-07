package com.catalinionescu.miniscript.exceptions;

public class LimitExceededException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public LimitExceededException() {
		super("Runtime Limit Exceeded");
	}
	
	public LimitExceededException(String text) {
		super(text);
	}
	
	public LimitExceededException(String message, Exception inner) {
		super(message, inner);
	}
}
