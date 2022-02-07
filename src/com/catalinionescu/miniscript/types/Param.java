package com.catalinionescu.miniscript.types;

public class Param {
	/// <summary>
	/// Param: helper class representing a function parameter.
	/// </summary>
	public String name;
	public Value defaultValue;

	public Param(String name, Value defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}
}
