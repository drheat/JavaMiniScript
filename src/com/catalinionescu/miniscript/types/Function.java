package com.catalinionescu.miniscript.types;

import java.util.ArrayList;
import java.util.List;

import com.catalinionescu.miniscript.Line;
import com.catalinionescu.miniscript.Machine;

/// <summary>
/// Function: our internal representation of a MiniScript function.  This includes
/// its parameters and its code.  (It does not include a name -- functions don't 
/// actually HAVE names; instead there are named variables whose value may happen 
/// to be a function.)
/// </summary>
public class Function {
	// Function parameters
	public List<Param> parameters;
	
	// Function code (compiled down to TAC form)
	public List<Line> code;

	public Function(List<Line> code) {
		this.code = code;
		parameters = new ArrayList<Param>();
	}
	
	@Override
	public String toString() {
		return toString(null);
	}

	public String toString(Machine vm) {
		StringBuilder s = new StringBuilder();
		s.append("FUNCTION(");			
		for (int i=0; i < parameters.size(); i++) {
			if (i > 0) s.append(", ");
			s.append(parameters.get(i).name);
			if (parameters.get(i).defaultValue != null) s.append("=" + parameters.get(i).defaultValue.CodeForm(vm));
		}
		s.append(")");
		return s.toString();
	}
}
