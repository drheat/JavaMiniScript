package com.catalinionescu.miniscript.exceptions;

public class SourceLoc {
	public String context;	// file name, etc. (optional)
	public int lineNum;

	public SourceLoc(String context, int lineNum) {
		this.context = context;
		this.lineNum = lineNum;
	}

	@Override
	public String toString() {
		return String.format("[%sline %d]", context == null || context.isEmpty() ? "" : context + " ", lineNum);
	}
}
