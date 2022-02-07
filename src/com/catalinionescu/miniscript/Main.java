package com.catalinionescu.miniscript;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.catalinionescu.miniscript.exceptions.LexerException;

public class Main {	
	public void printMenu() {
		System.out.println();
		System.out.println("Menu: [1] Lexer Unit Tests  [2] Parser Unit Tests  [3] Test Suite         [9] REPL  [0] Exit");
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
					runTestSuite();
					break;
				case "9":
					runREPL(input);
					break;
				case "0":
					System.out.println();
					System.out.println("Bye!");
					return;
			}
		}
	}
	
	private void runTest(List<String> sourceLines, int sourceLineNum, List<String> expectedOutput, int outputLineNum) {
		if (expectedOutput == null) expectedOutput = new ArrayList<>();

		Interpreter miniscript = new Interpreter(sourceLines);
		List<String> actualOutput = new ArrayList<>();
		miniscript.setStandardOutput((String s) -> actualOutput.add(s));
		miniscript.implicitOutput = miniscript.getStandardOutput();
		miniscript.errorOutput = miniscript.getStandardOutput();
		miniscript.RunUntilDone(60, false);
		
		int minLen = expectedOutput.size() < actualOutput.size() ? expectedOutput.size() : actualOutput.size();
		for (int i = 0; i < minLen; i++) {
			if (!actualOutput.get(i).equals(expectedOutput.get(i))) {
				System.out.println(String.format("TEST FAILED AT LINE %d\n  EXPECTED: %s\n    ACTUAL: %s", outputLineNum + i, expectedOutput.get(i), actualOutput.get(i)));
			}
		}
		
		if (expectedOutput.size() > actualOutput.size()) {
			System.out.println(String.format("TEST FAILED: MISSING OUTPUT AT LINE %d", outputLineNum + actualOutput.size()));
			for (int i = actualOutput.size(); i < expectedOutput.size(); i++) {
				System.out.println("  MISSING: " + expectedOutput.get(i));
			}
		} else if (actualOutput.size() > expectedOutput.size()) {
			System.out.println(String.format("TEST FAILED: EXTRA OUTPUT AT LINE %d", outputLineNum + expectedOutput.size()));
			for (int i = expectedOutput.size(); i < actualOutput.size(); i++) {
				System.out.println("  EXTRA: " + actualOutput.get(i));
			}
		}
	}

	private void runTestSuite() {
		System.out.println("*****************************************************************************");
		System.out.println("Running Test Suite");
		System.out.println();

		List<String> sourceLines = null;
		List<String> expectedOutput = null;
		int testLineNum = 0;
		int outputLineNum = 0;

		try (BufferedReader br = new BufferedReader(new FileReader("TestSuite.txt"))) {
		    String line;
			int lineNum = 1;
		    while ((line = br.readLine()) != null) {
				if (line.startsWith("====")) {
					if (sourceLines != null) {
						try {
							runTest(sourceLines, testLineNum, expectedOutput, outputLineNum);
						} catch (NullPointerException e) {
							System.out.println(lineNum);
							e.printStackTrace();
						}
					}
					sourceLines = null;
					expectedOutput = null;
				} else if (line.startsWith("----")) {
					expectedOutput = new ArrayList<>();
					outputLineNum = lineNum + 1;
				} else if (expectedOutput != null) {
					expectedOutput.add(line);
				} else {
					if (sourceLines == null) {
						sourceLines = new ArrayList<>();
						testLineNum = lineNum;
					}
					sourceLines.add(line);
				}
				
				lineNum++;
		    }
		} catch (IOException e) {
			System.out.println("Unable to read TestSuite.txt");
		}
		if (sourceLines != null) runTest(sourceLines, testLineNum, expectedOutput, outputLineNum);

		System.out.println("\nIntegration tests complete.\n");
	}
	
	public void runFile(String path) {
		runFile(path, false);
	}
	
	public void runFile(String path, boolean dumpTAC) {
		List<String> sourceLines = new ArrayList<>();
		try {
			sourceLines = Files.readAllLines(Paths.get(path));
		} catch (IOException e) {
			System.out.println("Unable to read: " + path);
			return;
		}
		
		Interpreter miniscript = new Interpreter(sourceLines);
		miniscript.setStandardOutput((String s) -> System.out.println(s));
		miniscript.implicitOutput = miniscript.getStandardOutput();
		miniscript.Compile();

		if (dumpTAC) {
			miniscript.vm.DumpTopContext();
		}		
		while (!miniscript.done()) {
			miniscript.RunUntilDone();
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
