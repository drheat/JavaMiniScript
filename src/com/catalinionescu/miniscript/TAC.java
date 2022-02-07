package com.catalinionescu.miniscript;

import java.util.List;
import com.catalinionescu.miniscript.intrinsics.Intrinsic;
import com.catalinionescu.miniscript.types.ValFunction;
import com.catalinionescu.miniscript.types.ValNumber;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.ValTemp;
import com.catalinionescu.miniscript.types.ValVar;

public class TAC {
	public static void Dump(List<Line> lines, int lineNumToHighlight) {
		Dump(lines, lineNumToHighlight, 0);
	}

	public static void Dump(List<Line> lines, int lineNumToHighlight, int indent) {
		int lineNum = 0;
		for (Line line : lines) {
			String s = (lineNum == lineNumToHighlight ? "> " : "  ") + (lineNum++) + ". ";
			System.out.println(s + line);
			if (line.op == Line.Op.BindAssignA) {
				ValFunction func = (ValFunction)line.rhsA;
				Dump(func.function.code, -1, indent+1);
			}
		}
	}

	public static ValTemp LTemp(int tempNum) {
		return new ValTemp(tempNum);
	}
	public static ValVar LVar(String identifier) {
		if (identifier == "self") return ValVar.self;
		return new ValVar(identifier);
	}
	public static ValTemp RTemp(int tempNum) {
		return new ValTemp(tempNum);
	}
	public static ValNumber Num(double value) {
		return new ValNumber(value);
	}
	public static ValString Str(String value) {
		return new ValString(value);
	}
	public static ValNumber IntrinsicByName(String name) {
		return new ValNumber(Intrinsic.GetByName(name).id());
	}
	
}
