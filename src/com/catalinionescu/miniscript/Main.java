package com.catalinionescu.miniscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

	public static void main(String[] args) throws IOException {
		Interpreter repl = new Interpreter();
		repl.implicitOutput = repl.getStandardOutput();
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader input = new BufferedReader(isr);
		
		while (true) {
			System.out.print(repl.NeedMoreInput() ? ">>> " : "> ");
			String inp = input.readLine();
			if (inp == null) break;
			repl.REPL(inp);
		}
	}

}
