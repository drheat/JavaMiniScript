package com.catalinionescu.miniscript.exceptions;

public class LexerException extends MiniscriptException {
	private static final long serialVersionUID = 1L;

	public LexerException() {
		super("Lexer Error");
	}
	
	public LexerException(String text) {
		super(text);
	}
	
	public LexerException(String message, Exception inner) {
		super(message, inner);
	}
}
