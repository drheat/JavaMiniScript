package com.catalinionescu.miniscript;

public class Token {
	public Type type;
	public String text;	// may be null for things like operators, whose text is fixed
	public boolean afterSpace;
	
	public Token() {
		this(Type.Unknown, null);
	}
	
	public Token(Type type) {
		this(type, null);
	}

	public Token(Type type, String text) {
		this.type = type;
		this.text = text;
	}

	@Override
	public String toString() {
		if (text == null) {
			return type.toString();
		}
		
		return String.format("%s(%s)", type, text);
	}

	public static Token EOL = new Token(Type.EOL);
	

	public enum Type {
		Unknown,
		Keyword,
		Number,
		String,
		Identifier,
		OpAssign,
		OpPlus,
		OpMinus,
		OpTimes,
		OpDivide,
		OpMod,
		OpPower,
		OpEqual,
		OpNotEqual,
		OpGreater,
		OpGreatEqual,
		OpLesser,
		OpLessEqual,
		LParen,
		RParen,
		LSquare,
		RSquare,
		LCurly,
		RCurly,
		AddressOf,
		Comma,
		Dot,
		Colon,
		Comment,
		EOL
	}
}
