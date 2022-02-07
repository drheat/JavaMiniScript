package com.catalinionescu.miniscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.catalinionescu.miniscript.exceptions.LexerException;

public class Main {	
	public void printMenu() {
		System.out.println();
		System.out.println("Menu: [1] Lexer Unit Tests  [2] Parser Unit Tests  [3] REPL           [4] Exit");
		System.out.print("> ");
	}

	public static void main(String[] args) throws IOException {
		Main main = new Main();
		main.run();
	}

	private void run() throws IOException {
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader input = new BufferedReader(isr);
		
		while (true) {
			printMenu();
			String menu = input.readLine().trim();
			
			switch (menu) {
				case "1":
					runLexerTests();
					break;
				case "2":
					runParserTests();
					break;
				case "3":
					runREPL(input);
					break;
				case "4":
					System.out.println();
					System.out.println("Bye!");
					return;
			}
		}
	}

	private void runREPL(BufferedReader input) throws IOException {
		System.out.println("*****************************************************************************");
		System.out.println("Running REPL");
		System.out.println();

		Interpreter repl = new Interpreter();
		repl.implicitOutput = repl.getStandardOutput();
		
		while (true) {
			System.out.print(repl.NeedMoreInput() ? ">>> " : "> ");
			String inp = input.readLine();
			if (inp == null) break;
			repl.REPL(inp);
		}
	}

	private void runParserTests() {
		System.out.println("*****************************************************************************");
		System.out.println("Running PARSER tests");
		System.out.println();

		Parser.RunUnitTests();
	}

	private void runLexerTests() {		
		System.out.println("*****************************************************************************");
		System.out.println("Running LEXER tests");
		System.out.println();
		
		try {
			Lexer.RunUnitTests();
		} catch (LexerException e) {
			System.err.println("Lexer generated an exception: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
