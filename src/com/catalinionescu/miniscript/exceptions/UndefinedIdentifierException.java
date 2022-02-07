package com.catalinionescu.miniscript.exceptions;

public class UndefinedIdentifierException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	UndefinedIdentifierException() {
		// don't call this version!
	}

	public UndefinedIdentifierException(String ident) {
		super("Undefined Identifier: '" + ident + "' is unknown in this context");
	}

	public UndefinedIdentifierException(String message, Exception inner) {
		super(message, inner);
	}
}
