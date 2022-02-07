package com.catalinionescu.miniscript.exceptions;

public class KeyException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	KeyException() {}		// don't use this version

	public KeyException(String key) {
		super("Key Not Found: '" + key + "' not found in map");
	}

	public KeyException(String message, Exception inner) {
		super(message, inner);
	}
}
