package com.catalinionescu.miniscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.catalinionescu.miniscript.Interpreter.TextOutputMethod;
import com.catalinionescu.miniscript.Line.Op;
import com.catalinionescu.miniscript.exceptions.CompilerException;
import com.catalinionescu.miniscript.exceptions.LexerException;
import com.catalinionescu.miniscript.exceptions.MiniscriptException;
import com.catalinionescu.miniscript.exceptions.SourceLoc;
import com.catalinionescu.miniscript.intrinsics.Intrinsics;
import com.catalinionescu.miniscript.types.Function;
import com.catalinionescu.miniscript.types.Param;
import com.catalinionescu.miniscript.types.ValFunction;
import com.catalinionescu.miniscript.types.ValList;
import com.catalinionescu.miniscript.types.ValMap;
import com.catalinionescu.miniscript.types.ValNull;
import com.catalinionescu.miniscript.types.ValNumber;
import com.catalinionescu.miniscript.types.ValSeqElem;
import com.catalinionescu.miniscript.types.ValString;
import com.catalinionescu.miniscript.types.ValTemp;
import com.catalinionescu.miniscript.types.ValVar;
import com.catalinionescu.miniscript.types.Value;

public class Parser {
	public String errorContext;	// name of file, etc., used for error reporting
	//public int lineNum;			// which line number we're currently parsing

	// BackPatch: represents a place where we need to patch the code to fill
	// in a jump destination (once we figure out where that destination is).
	class BackPatch {
		public int lineNum;			// which code line to patch
		public String waitingFor;	// what keyword we're waiting for (e.g., "end if")
		
		public BackPatch(int lineNum, String waitingFor) {
			this.lineNum = lineNum;
			this.waitingFor = waitingFor;
		}
	}

	// JumpPoint: represents a place in the code we will need to jump to later
	// (typically, the top of a loop of some sort).
	class JumpPoint {
		public int lineNum;			// line number to jump to		
		public String keyword;		// jump type, by keyword: "while", "for", etc.
		
		public JumpPoint(int lineNum, String keyword) {
			this.lineNum = lineNum;
			this.keyword = keyword;
		}
	}

	class ParseState {
		public List<Line> code = new ArrayList<>();
		public List<BackPatch> backpatches = new ArrayList<>();
		public List<JumpPoint> jumpPoints = new ArrayList<>();
		public int nextTempNum = 0;

		public void Add(Line line) {
			code.add(line);
		}

		/// <summary>
		/// Add the last code line as a backpatch point, to be patched
		/// (in rhsA) when we encounter a line with the given waitFor.
		/// </summary>
		/// <param name="waitFor">Wait for.</param>
		public void AddBackpatch(String waitFor) {
			backpatches.add(new BackPatch(code.size() - 1, waitFor));
		}

		public void AddJumpPoint(String jumpKeyword) {
			jumpPoints.add(new JumpPoint(code.size(), jumpKeyword));
		}

		public JumpPoint CloseJumpPoint(String keyword) throws CompilerException {
			int idx = jumpPoints.size() - 1;
			if (idx < 0 || jumpPoints.get(idx).keyword != keyword) {
				throw new CompilerException(String.format("'end %s' without matching '%s'", keyword, keyword));
			}
			JumpPoint result = jumpPoints.get(idx);
			jumpPoints.remove(idx);
			return result;
		}

		// Return whether the given line is a jump target.
		public boolean IsJumpTarget(int lineNum) {
			for (int i=0; i < code.size(); i++) {
				Op op = code.get(i).op;
				if ((op == Line.Op.GotoA || op == Line.Op.GotoAifB 
				 || op == Line.Op.GotoAifNotB || op == Line.Op.GotoAifTrulyB)
				 && code.get(i).rhsA instanceof ValNumber && code.get(i).rhsA.IntValue() == lineNum) return true;
			}
			for (int i=0; i<jumpPoints.size(); i++) {
				if (jumpPoints.get(i).lineNum == lineNum) return true;
			}
			return false;
		}
		
		public void Patch(String keywordFound) throws CompilerException {
			Patch(keywordFound, 0);
		}

		/// <summary>
		/// Call this method when we've found an 'end' keyword, and want
		/// to patch up any jumps that were waiting for that.  Patch the
		/// matching backpatch (and any after it) to the current code end.
		/// </summary>
		/// <param name="keywordFound">Keyword found.</param>
		/// <param name="reservingLines">Extra lines (after the current position) to patch to.</param> 
		public void Patch(String keywordFound, int reservingLines) throws CompilerException {
			Patch(keywordFound, false, reservingLines);
		}
		
		public void Patch(String keywordFound, boolean alsoBreak) throws CompilerException {
			Patch(keywordFound, alsoBreak, 0);
		}

		/// <summary>
		/// Call this method when we've found an 'end' keyword, and want
		/// to patch up any jumps that were waiting for that.  Patch the
		/// matching backpatch (and any after it) to the current code end.
		/// </summary>
		/// <param name="keywordFound">Keyword found.</param>
		/// <param name="alsoBreak">If true, also patch "break"; otherwise skip it.</param> 
		/// <param name="reservingLines">Extra lines (after the current position) to patch to.</param> 
		public void Patch(String keywordFound, boolean alsoBreak, int reservingLines) throws CompilerException {
			Value target = TAC.Num(code.size() + reservingLines);
			boolean done = false;
			for (int idx = backpatches.size() - 1; idx >= 0 && !done; idx--) {
				boolean patchIt = false;
				if (backpatches.get(idx).waitingFor == keywordFound) patchIt = done = true;
				else if (backpatches.get(idx).waitingFor == "break") {
					// Not the expected keyword, but "break"; this is always OK,
					// but we may or may not patch it depending on the call.
					patchIt = alsoBreak;
				} else {
					// Not the expected patch, and not "break"; we have a mismatched block start/end.
					throw new CompilerException("'" + keywordFound + "' skips expected '" + backpatches.get(idx).waitingFor + "'");
				}
				if (patchIt) {
					code.get(backpatches.get(idx).lineNum).rhsA = target;
					backpatches.remove(idx);
				}
			}
			// Make sure we found one...
			if (!done) throw new CompilerException("'" + keywordFound + "' without matching block starter");
		}

		/// <summary>
		/// Patches up all the branches for a single open if block.  That includes
		/// the last "else" block, as well as one or more "end if" jumps.
		/// </summary>
		public void PatchIfBlock() throws CompilerException {
			Value target = TAC.Num(code.size());

			int idx = backpatches.size() - 1;
			while (idx >= 0) {
				BackPatch bp = backpatches.get(idx);
				if (bp.waitingFor == "if:MARK") {
					// There's the special marker that indicates the true start of this if block.
					backpatches.remove(idx);
					return;
				} else if (bp.waitingFor == "end if" || bp.waitingFor == "else") {
					code.get(bp.lineNum).rhsA = target;
					backpatches.remove(idx);
				} else if (backpatches.get(idx).waitingFor == "break") {
					// Not the expected keyword, but "break"; this is always OK.
				} else {
					// Not the expected patch, and not "break"; we have a mismatched block start/end.
					throw new CompilerException("'end if' without matching 'if'");
				}
				idx--;
			}
			// If we get here, we never found the expected if:MARK.  That's an error.
			throw new CompilerException("'end if' without matching 'if'");
		}
	}
	
	// Partial input, in the case where line continuation has been used.
	String partialInput;

	// List of open code blocks we're working on (while compiling a function,
	// we push a new one onto this stack, compile to that, and then pop it
	// off when we reach the end of the function).
	Stack<ParseState> outputStack;

	// Handy reference to the top of outputStack.
	ParseState output;

	// A new parse state that needs to be pushed onto the stack, as soon as we
	// finish with the current line we're working on:
	ParseState pendingState = null;

	public Parser() {
		Reset();
	}

	/// <summary>
	/// Completely clear out and reset our parse state, throwing out
	/// any code and intermediate results.
	/// </summary>
	public void Reset() {
		output = new ParseState();
		if (outputStack == null) outputStack = new Stack<ParseState>();
		else outputStack.clear();
		outputStack.push(output);
	}

	/// <summary>
	/// Partially reset, abandoning backpatches, but keeping already-
	/// compiled code.  This would be used in a REPL, when the user
	/// may want to reset and continue after a botched loop or function.
	/// </summary>
	public void PartialReset() {
		if (outputStack == null) outputStack = new Stack<ParseState>();
		while (outputStack.size() > 1) outputStack.pop();
		output = outputStack.peek();
		output.backpatches.clear();
		output.jumpPoints.clear();
		output.nextTempNum = 0;
		partialInput = null;
		pendingState = null;
	}

	public boolean NeedMoreInput() {
		if (!(partialInput == null || partialInput.isEmpty())) return true;
		if (outputStack.size() > 1) return true;
		if (output.backpatches.size() > 0) return true;
		return false;
	}

	/// <summary>
	/// Return whether the given source code ends in a token that signifies that
	/// the statement continues on the next line.  That includes binary operators,
	/// open brackets or parentheses, etc.
	/// </summary>
	/// <param name="sourceCode">source code to analyze</param>
	/// <returns>true if line continuation is called for; false otherwise</returns>
	public static boolean EndsWithLineContinuation(String sourceCode) {
		try {
			Token lastTok = Lexer.LastToken(sourceCode);
			// Almost any token at the end will signify line continuation, except:
			switch (lastTok.type) {
				case EOL:
				case Identifier:
				case Keyword:
				case Number:
				case RCurly:
				case RParen:
				case RSquare:
				case String:
				case Unknown:
					return false;
				default:
					return true;
			}
		} catch (LexerException e) {
			return false;
		}
	}
	
	public void Parse(String sourceCode) throws CompilerException, LexerException {
		Parse(sourceCode, false);
	}

	public void Parse(String sourceCode, boolean replMode) throws CompilerException, LexerException {
		if (replMode) {
			// Check for an incomplete final line by finding the last (non-comment) token.
			boolean isPartial = EndsWithLineContinuation(sourceCode);
			if (isPartial) {
				partialInput += Lexer.TrimComment(sourceCode);
				return;
			}
		}
		Lexer tokens = new Lexer(partialInput + sourceCode);
		partialInput = null;
		ParseMultipleLines(tokens);

		if (!replMode && NeedMoreInput()) {
			// Whoops, we need more input but we don't have any.  This is an error.
			tokens.lineNum++;	// (so we report PAST the last line, making it clear this is an EOF problem)
			if (outputStack.size() > 1) {
				throw new CompilerException(errorContext, tokens.lineNum, "'function' without matching 'end function'");
			} else if (output.backpatches.size() > 0) {
				BackPatch bp = output.backpatches.get(output.backpatches.size() - 1);
				String msg;
				switch (bp.waitingFor) {
				case "end for":
					msg = "'for' without matching 'end for'";
					break;
				case "end if":
					msg = "'if' without matching 'end if'";
					break;
				case "end while":
					msg = "'while' without matching 'end while'";
					break;
				default:
					msg = "unmatched block opener";
					break;
				}
				throw new CompilerException(errorContext, tokens.lineNum, msg);
			}
		}
	}

	/// <summary>
	/// Create a virtual machine loaded with the code we have parsed.
	/// </summary>
	/// <param name="standardOutput"></param>
	/// <returns></returns>
	public Machine CreateVM(TextOutputMethod standardOutput) {
		Context root = new Context(output.code);
		return new Machine(root, standardOutput);
	}
	
	/// <summary>
	/// Create a Function with the code we have parsed, for use as
	/// an import.  That means, it runs all that code, then at the
	/// end it returns `locals` so that the caller can get its symbols.
	/// </summary>
	/// <returns></returns>
	public Function CreateImport() {
		// Add one additional line to return `locals` as the function return value.
		ValVar locals = new ValVar("locals");
		output.Add(new Line(TAC.LTemp(0), Line.Op.ReturnA, locals));
		// Then wrap the whole thing in a Function.
		Function result = new Function(output.code);
		return result;
	}

	public void REPL(String line) throws MiniscriptException {
		Parse(line);
	
		Machine vm = CreateVM(null);
		while (!vm.done()) vm.Step();
	}

	void AllowLineBreak(Lexer tokens) throws LexerException {
		while (tokens.Peek().type == Token.Type.EOL && !tokens.AtEnd()) tokens.Dequeue();
	}

	interface ExpressionParsingMethod {
		public default Value parse(Lexer tokens) {
			return parse(tokens, false, false);
		}
		
		public default Value parse(Lexer tokens, boolean asLval) {
			return parse(tokens, asLval, false);
		}
		
		public Value parse(Lexer tokens, boolean asLval, boolean statementStart);
	}

	/// <summary>
	/// Parse multiple statements until we run out of tokens, or reach 'end function'.
	/// </summary>
	/// <param name="tokens">Tokens.</param>
	void ParseMultipleLines(Lexer tokens) throws LexerException, CompilerException {
		while (!tokens.AtEnd()) {
			// Skip any blank lines
			if (tokens.Peek().type == Token.Type.EOL) {
				tokens.Dequeue();
				continue;
			}

			// Prepare a source code location for error reporting
			SourceLoc location = new SourceLoc(errorContext, tokens.lineNum);

			// Pop our context if we reach 'end function'.
			if (tokens.Peek().type == Token.Type.Keyword && tokens.Peek().text == "end function") {
				tokens.Dequeue();
				if (outputStack.size() > 1) {
					outputStack.pop();
					output = outputStack.peek();
				} else {
					CompilerException e = new CompilerException("'end function' without matching block starter");
					e.location = location;
					throw e;
				}
				continue;
			}

			// Parse one line (statement).
			int outputStart = output.code.size();
			try {
				ParseStatement(tokens);
			} catch (MiniscriptException mse) {
				if (mse.location == null) mse.location = location;
				throw mse;
			}
			// Fill in the location info for all the TAC lines we just generated.
			for (int i = outputStart; i < output.code.size(); i++) {
				output.code.get(i).location = location;
			}
		}
	}
	
	void ParseStatement(Lexer tokens) throws LexerException, CompilerException {
		ParseStatement(tokens, false);
	}

	void ParseStatement(Lexer tokens, boolean allowExtra) throws LexerException, CompilerException {
		if (tokens.Peek().type == Token.Type.Keyword && tokens.Peek().text != "not"
			&& tokens.Peek().text != "true" && tokens.Peek().text != "false") {
			// Handle statements that begin with a keyword.
			String keyword = tokens.Dequeue().text;
			switch (keyword) {
			case "return":
				Value returnValue = null;
				if (tokens.Peek().type != Token.Type.EOL) {
					returnValue = ParseExpr(tokens);
				}
				output.Add(new Line(TAC.LTemp(0), Line.Op.ReturnA, returnValue));
				break;
			case "if":
				Value condition = ParseExpr(tokens);
				RequireToken(tokens, Token.Type.Keyword, "then");
				// OK, now we need to emit a conditional branch, but keep track of this
				// on a stack so that when we get the corresponding "else" or  "end if", 
				// we can come back and patch that jump to the right place.
				output.Add(new Line(null, Line.Op.GotoAifNotB, null, condition));

				// ...but if blocks also need a special marker in the backpack stack
				// so we know where to stop when patching up (possibly multiple) 'end if' jumps.
				// We'll push a special dummy backpatch here that we look for in PatchIfBlock.
				output.AddBackpatch("if:MARK");
				output.AddBackpatch("else");
				
				// Allow for the special one-statement if: if the next token after "then"
				// is not EOL, then parse a statement, and do the same for any else or
				// else-if blocks, until we get to EOL (and then implicitly do "end if").
				if (tokens.Peek().type != Token.Type.EOL) {
					ParseStatement(tokens, true);  // parses a single statement for the "then" body
					if (tokens.Peek().type == Token.Type.Keyword && tokens.Peek().text == "else") {
						tokens.Dequeue();	// skip "else"
						StartElseClause();
						ParseStatement(tokens, true);		// parse a single statement for the "else" body
					} else {
						RequireEitherToken(tokens, Token.Type.Keyword, "else", Token.Type.EOL);
					}
					output.PatchIfBlock();	// terminate the single-line if
				} else {
					tokens.Dequeue();	// skip EOL
				}
				return;
			case "else":
				StartElseClause();
				break;
			case "else if":
					StartElseClause();
					Value cond = ParseExpr(tokens);
					RequireToken(tokens, Token.Type.Keyword, "then");
					output.Add(new Line(null, Line.Op.GotoAifNotB, null, cond));
					output.AddBackpatch("else");
				break;
			case "end if":
				// OK, this is tricky.  We might have an open "else" block or we might not.
				// And, we might have multiple open "end if" jumps (one for the if part,
				// and another for each else-if part).  Patch all that as a special case.
				output.PatchIfBlock();
				break;
			case "while":
				// We need to note the current line, so we can jump back up to it at the end.
				output.AddJumpPoint(keyword);

				// Then parse the condition.
				Value condW = ParseExpr(tokens);

				// OK, now we need to emit a conditional branch, but keep track of this
				// on a stack so that when we get the corresponding "end while", 
				// we can come back and patch that jump to the right place.
				output.Add(new Line(null, Line.Op.GotoAifNotB, null, condW));
				output.AddBackpatch("end while");
				break;
			case "end while":
				// Unconditional jump back to the top of the while loop.
				JumpPoint jump = output.CloseJumpPoint("while");
				output.Add(new Line(null, Line.Op.GotoA, TAC.Num(jump.lineNum)));
				// Then, backpatch the open "while" branch to here, right after the loop.
				// And also patch any "break" branches emitted after that point.
				output.Patch(keyword, true);
				break;
			case "for":
				// Get the loop variable, "in" keyword, and expression to loop over.
				// (Note that the expression is only evaluated once, before the loop.)
				Token loopVarTok = RequireToken(tokens, Token.Type.Identifier);
				ValVar loopVar = new ValVar(loopVarTok.text);
				RequireToken(tokens, Token.Type.Keyword, "in");
				Value stuff = ParseExpr(tokens);
				if (stuff == null) {
					throw new CompilerException(errorContext, tokens.lineNum, "sequence expression expected for 'for' loop");
				}

				// Create an index variable to iterate over the sequence, initialized to -1.
				ValVar idxVar = new ValVar("__" + loopVarTok.text + "_idx");
				output.Add(new Line(idxVar, Line.Op.AssignA, TAC.Num(-1)));

				// We need to note the current line, so we can jump back up to it at the end.
				output.AddJumpPoint(keyword);

				// Now increment the index variable, and branch to the end if it's too big.
				// (We'll have to backpatch this branch later.)
				output.Add(new Line(idxVar, Line.Op.APlusB, idxVar, TAC.Num(1)));
				ValTemp sizeOfSeq = new ValTemp(output.nextTempNum++);
				output.Add(new Line(sizeOfSeq, Line.Op.LengthOfA, stuff));
				ValTemp isTooBig = new ValTemp(output.nextTempNum++);
				output.Add(new Line(isTooBig, Line.Op.AGreatOrEqualB, idxVar, sizeOfSeq));
				output.Add(new Line(null, Line.Op.GotoAifB, null, isTooBig));
				output.AddBackpatch("end for");

				// Otherwise, get the sequence value into our loop variable.
				output.Add(new Line(loopVar, Line.Op.ElemBofIterA, stuff, idxVar));
				break;
			case "end for":
				// Unconditional jump back to the top of the for loop.
				JumpPoint jumpEF = output.CloseJumpPoint("for");
				output.Add(new Line(null, Line.Op.GotoA, TAC.Num(jumpEF.lineNum)));
				// Then, backpatch the open "for" branch to here, right after the loop.
				// And also patch any "break" branches emitted after that point.
				output.Patch(keyword, true);
				break;
			case "break":
				// Emit a jump to the end, to get patched up later.
				output.Add(new Line(null, Line.Op.GotoA));
				output.AddBackpatch("break");
				break;
			case "continue":
				// Jump unconditionally back to the current open jump point.
				if (output.jumpPoints.size() == 0) {
					throw new CompilerException(errorContext, tokens.lineNum, "'continue' without open loop block");
				}
				JumpPoint jumpC = output.jumpPoints.get(output.jumpPoints.size() - 1);
				output.Add(new Line(null, Line.Op.GotoA, TAC.Num(jumpC.lineNum)));
				break;
			default:
				throw new CompilerException(errorContext, tokens.lineNum,
					"unexpected keyword '" + keyword + "' at start of line");
			}
		} else {
			ParseAssignment(tokens, allowExtra);
		}

		// A statement should consume everything to the end of the line.
		if (!allowExtra) RequireToken(tokens, Token.Type.EOL);

		// Finally, if we have a pending state, because we encountered a function(),
		// then push it onto our stack now that we're done with that statement.
		if (pendingState != null) {
			output = pendingState;
			outputStack.push(output);
			pendingState = null;
		}

	}
	
	void StartElseClause() throws CompilerException {
		// Back-patch the open if block, but leaving room for the jump:
		// Emit the jump from the current location, which is the end of an if-block,
		// to the end of the else block (which we'll have to back-patch later).
		output.Add(new Line(null, Line.Op.GotoA, null));
		// Back-patch the previously open if-block to jump here (right past the goto).
		output.Patch("else");
		// And open a new back-patch for this goto (which will jump all the way to the end if).
		output.AddBackpatch("end if");
	}
	
	void ParseAssignment(Lexer tokens) throws LexerException, CompilerException {
		ParseAssignment(tokens, false);
	}

	void ParseAssignment(Lexer tokens, boolean allowExtra) throws LexerException, CompilerException {
		Value expr = ParseExpr(tokens, true, true);
		Value lhs, rhs;
		Token peek = tokens.Peek();
		if (peek.type == Token.Type.EOL ||
				(peek.type == Token.Type.Keyword && peek.text == "else")) {
			// No explicit assignment; store an implicit result
			rhs = FullyEvaluate(expr);
			output.Add(new Line(null, Line.Op.AssignImplicit, rhs));
			return;
		}
		if (peek.type == Token.Type.OpAssign) {
			tokens.Dequeue();	// skip '='
			lhs = expr;
			rhs = ParseExpr(tokens);
		} else {
			// This looks like a command statement.  Parse the rest
			// of the line as arguments to a function call.
			Value funcRef = expr;
			int argCount = 0;
			while (true) {
				Value arg = ParseExpr(tokens);
				output.Add(new Line(null, Line.Op.PushParam, arg));
				argCount++;
				if (tokens.Peek().type == Token.Type.EOL) break;
				if (tokens.Peek().type == Token.Type.Keyword && tokens.Peek().text == "else") break;
				if (tokens.Peek().type == Token.Type.Comma) {
					tokens.Dequeue();
					AllowLineBreak(tokens);
					continue;
				}
				if (RequireEitherToken(tokens, Token.Type.Comma, Token.Type.EOL).type == Token.Type.EOL) break;
			}
			ValTemp result = new ValTemp(output.nextTempNum++);
			output.Add(new Line(result, Line.Op.CallFunctionA, funcRef, TAC.Num(argCount)));					
			output.Add(new Line(null, Line.Op.AssignImplicit, result));
			return;
		}

		// OK, now, in many cases our last TAC line at this point is an assignment to our RHS temp.
		// In that case, as a simple (but very useful) optimization, we can simply patch that to 
		// assign to our lhs instead.  BUT, we must not do this if there are any jumps to the next
		// line, as may happen due to short-cut evaluation (issue #6).
		if (rhs instanceof ValTemp && output.code.size() > 0 && !output.IsJumpTarget(output.code.size())) {			
			Line line = output.code.get(output.code.size() - 1);
			if (line.lhs.equals(rhs)) {
				// Yep, that's the case.  Patch it up.
				line.lhs = lhs;
				return;
			}
		}
		
        // If the last line was us creating and assigning a function, then we don't add a second assign
        // op, we instead just update that line with the proper LHS
        if (rhs instanceof ValFunction && output.code.size() > 0) {
            Line line = output.code.get(output.code.size() - 1);
            if (line.op == Line.Op.BindAssignA) {
                line.lhs = lhs;
                return;
            }
        }

		// In any other case, do an assignment statement to our lhs.
		output.Add(new Line(lhs, Line.Op.AssignA, rhs));
	}
	
	Value ParseExpr(Lexer tokens)  throws LexerException, CompilerException {
		return ParseExpr(tokens, false, false);		
	}
	
	Value ParseExpr(Lexer tokens, boolean asLval)  throws LexerException, CompilerException {
		return ParseExpr(tokens, asLval, false);
	}

	Value ParseExpr(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		// ExpressionParsingMethod nextLevel = this::ParseFunction;
		//return nextLevel.parse(tokens, asLval, statementStart);
		return ParseFunction(tokens, asLval, statementStart);
	}
	
	Value ParseFunction(Lexer tokens) throws LexerException, CompilerException {
		return ParseFunction(tokens, false, false);
	}
	
	Value ParseFunction(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseFunction(tokens, asLval, false);
	}

	Value ParseFunction(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		// ExpressionParsingMethod nextLevel = this::ParseOr;
		Token tok = tokens.Peek();
		if (tok.type != Token.Type.Keyword || tok.text != "function") return ParseOr(tokens, asLval, statementStart);
		tokens.Dequeue();

		Function func = new Function(null);
		tok = tokens.Peek();
		if (tok.type != Token.Type.EOL) { 
			Token paren = RequireToken(tokens, Token.Type.LParen);
			while (tokens.Peek().type != Token.Type.RParen) {
				// parse a parameter: a comma-separated list of
				//			identifier
				//	or...	identifier = expr
				Token id = tokens.Dequeue();
				if (id.type != Token.Type.Identifier) throw new CompilerException(errorContext, tokens.lineNum, "got " + id + " where an identifier is required");
				Value defaultValue = null;
				if (tokens.Peek().type == Token.Type.OpAssign) {
					tokens.Dequeue();	// skip '='
					defaultValue = ParseExpr(tokens);
				}
				func.parameters.add(new Param(id.text, defaultValue));
				if (tokens.Peek().type == Token.Type.RParen) break;
				RequireToken(tokens, Token.Type.Comma);
			}

			RequireToken(tokens, Token.Type.RParen);
		}

		// Now, we need to parse the function body into its own parsing context.
		// But don't push it yet -- we're in the middle of parsing some expression
		// or statement in the current context, and need to finish that.
		if (pendingState != null) throw new CompilerException(errorContext, tokens.lineNum, "can't start two functions in one statement");
		pendingState = new ParseState();
		pendingState.nextTempNum = 1;	// (since 0 is used to hold return value)

//		Console.WriteLine("STARTED FUNCTION");

		// Create a function object attached to the new parse state code.
		func.code = pendingState.code;
		ValFunction valFunc = new ValFunction(func);
		output.Add(new Line(null, Line.Op.BindAssignA, valFunc));
		return valFunc;
	}
	
	Value ParseOr(Lexer tokens) throws LexerException, CompilerException {
		return ParseOr(tokens, false, false);
	}
	
	Value ParseOr(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseOr(tokens, asLval, false);
	}

	Value ParseOr(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		// ExpressionParsingMethod nextLevel = this::ParseAnd;
		Value val = ParseAnd(tokens, asLval, statementStart);
		List<Line> jumpLines = null;
		Token tok = tokens.Peek();
		while (tok.type == Token.Type.Keyword && tok.text == "or") {
			tokens.Dequeue();		// discard "or"
			val = FullyEvaluate(val);

			AllowLineBreak(tokens); // allow a line break after a binary operator

			// Set up a short-circuit jump based on the current value; 
			// we'll fill in the jump destination later.  Note that the
			// usual GotoAifB opcode won't work here, without breaking
			// our calculation of intermediate truth.  We need to jump
			// only if our truth value is >= 1 (i.e. absolutely true).
			Line jump = new Line(null, Line.Op.GotoAifTrulyB, null, val);
			output.Add(jump);
			if (jumpLines == null) jumpLines = new ArrayList<>();
			jumpLines.add(jump);

			Value opB = ParseAnd(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), Line.Op.AOrB, val, opB));
			val = TAC.RTemp(tempNum);

			tok = tokens.Peek();
		}

		// Now, if we have any short-circuit jumps, those are going to need
		// to copy the short-circuit result (always 1) to our output temp.
		// And anything else needs to skip over that.  So:
		if (jumpLines != null) {
			output.Add(new Line(null, Line.Op.GotoA, TAC.Num(output.code.size() + 2)));	// skip over this line:
			output.Add(new Line(val, Line.Op.AssignA, ValNumber.one));	// result = 1
			for (Line jump : jumpLines) {
				jump.rhsA = TAC.Num(output.code.size() - 1);	// short-circuit to the above result=1 line
			}
		}

		return val;
	}
	
	Value ParseAnd(Lexer tokens) throws LexerException, CompilerException {
		return ParseAnd(tokens, false, false);
	}
	
	Value ParseAnd(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseAnd(tokens, asLval, false);
	}

	Value ParseAnd(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		// ExpressionParsingMethod nextLevel = this::ParseNot;
		Value val = ParseNot(tokens, asLval, statementStart);
		List<Line> jumpLines = null;
		Token tok = tokens.Peek();
		while (tok.type == Token.Type.Keyword && tok.text == "and") {
			tokens.Dequeue();		// discard "and"
			val = FullyEvaluate(val);

			AllowLineBreak(tokens); // allow a line break after a binary operator

			// Set up a short-circuit jump based on the current value; 
			// we'll fill in the jump destination later.
			Line jump = new Line(null, Line.Op.GotoAifNotB, null, val);
			output.Add(jump);
			if (jumpLines == null) jumpLines = new ArrayList<>();
			jumpLines.add(jump);

			Value opB = ParseNot(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), Line.Op.AAndB, val, opB));
			val = TAC.RTemp(tempNum);

			tok = tokens.Peek();
		}

		// Now, if we have any short-circuit jumps, those are going to need
		// to copy the short-circuit result (always 0) to our output temp.
		// And anything else needs to skip over that.  So:
		if (jumpLines != null) {
			output.Add(new Line(null, Line.Op.GotoA, TAC.Num(output.code.size() + 2)));	// skip over this line:
			output.Add(new Line(val, Line.Op.AssignA, ValNumber.zero));	// result = 0
			for (Line jump : jumpLines) {
				jump.rhsA = TAC.Num(output.code.size() - 1);	// short-circuit to the above result=0 line
			}
		}

		return val;
	}
	
	Value ParseNot(Lexer tokens) throws LexerException, CompilerException {
		return ParseNot(tokens, false, false);
	}
	
	Value ParseNot(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseNot(tokens, asLval, false);
	}

	Value ParseNot(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseIsA;
		Token tok = tokens.Peek();
		Value val;
		if (tok.type == Token.Type.Keyword && tok.text == "not") {
			tokens.Dequeue();		// discard "not"

			AllowLineBreak(tokens); // allow a line break after a unary operator

			val = ParseIsA(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), Line.Op.NotA, val));
			val = TAC.RTemp(tempNum);
		} else {
			val = ParseIsA(tokens, asLval, statementStart);
		}
		return val;
	}
	
	Value ParseIsA(Lexer tokens) throws LexerException, CompilerException {
		return ParseIsA(tokens, false, false);
	}
	
	Value ParseIsA(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseIsA(tokens, asLval, false);
	}

	Value ParseIsA(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		// ExpressionParsingMethod nextLevel = this::ParseComparisons;
		Value val = ParseComparisons(tokens, asLval, statementStart);
		if (tokens.Peek().type == Token.Type.Keyword && tokens.Peek().text == "isa") {
			tokens.Dequeue();		// discard the isa operator
			AllowLineBreak(tokens); // allow a line break after a binary operator
			val = FullyEvaluate(val);
			Value opB = ParseComparisons(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), Line.Op.AisaB, val, opB));
			val = TAC.RTemp(tempNum);
		}
		return val;
	}
	
	Value ParseComparisons(Lexer tokens) throws LexerException, CompilerException {
		return ParseComparisons(tokens, false, false);
	}
	
	Value ParseComparisons(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseComparisons(tokens, asLval, false);
	}
	
	Value ParseComparisons(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseAddSub;
		Value val = ParseAddSub(tokens, asLval, statementStart);
		Value opA = val;
		Line.Op opcode = ComparisonOp(tokens.Peek().type);
		// Parse a string of comparisons, all multiplied together
		// (so every comparison must be true for the whole expression to be true).
		boolean firstComparison = true;
		while (opcode != Line.Op.Noop) {
			tokens.Dequeue();	// discard the operator (we have the opcode)
			opA = FullyEvaluate(opA);

			AllowLineBreak(tokens); // allow a line break after a binary operator

			Value opB = ParseAddSub(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), opcode,	opA, opB));
			if (firstComparison) {
				firstComparison = false;
			} else {
				tempNum = output.nextTempNum++;
				output.Add(new Line(TAC.LTemp(tempNum), Line.Op.ATimesB, val, TAC.RTemp(tempNum - 1)));
			}
			val = TAC.RTemp(tempNum);
			opA = opB;
			opcode = ComparisonOp(tokens.Peek().type);
		}
		return val;
	}

	// Find the TAC operator that corresponds to the given token type,
	// for comparisons.  If it's not a comparison operator, return Line.Op.Noop.
	static Line.Op ComparisonOp(Token.Type tokenType) {
		switch (tokenType) {
			case OpEqual:		return Line.Op.AEqualB;
			case OpNotEqual:	return Line.Op.ANotEqualB;
			case OpGreater:		return Line.Op.AGreaterThanB;
			case OpGreatEqual:	return Line.Op.AGreatOrEqualB;
			case OpLesser:		return Line.Op.ALessThanB;
			case OpLessEqual:	return Line.Op.ALessOrEqualB;
			default: return Line.Op.Noop;
		}
	}
	
	Value ParseAddSub(Lexer tokens) throws LexerException, CompilerException {
		return ParseAddSub(tokens, false, false);
	}
	
	Value ParseAddSub(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseAddSub(tokens, asLval, false);
	}

	Value ParseAddSub(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseMultDiv;
		Value val = ParseMultDiv(tokens, asLval, statementStart);
		Token tok = tokens.Peek();
		while (tok.type == Token.Type.OpPlus || 
				(tok.type == Token.Type.OpMinus
				&& (!statementStart || !tok.afterSpace  || tokens.IsAtWhitespace()))) {
			tokens.Dequeue();

			AllowLineBreak(tokens); // allow a line break after a binary operator

			val = FullyEvaluate(val);
			Value opB = ParseMultDiv(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), 
				tok.type == Token.Type.OpPlus ? Line.Op.APlusB : Line.Op.AMinusB, val, opB));
			val = TAC.RTemp(tempNum);

			tok = tokens.Peek();
		}
		return val;
	}
	
	Value ParseMultDiv(Lexer tokens) throws LexerException, CompilerException {
		return ParseMultDiv(tokens, false, false);
	}
	
	Value ParseMultDiv(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseMultDiv(tokens, asLval, false);
	}

	Value ParseMultDiv(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseUnaryMinus;
		Value val = ParseUnaryMinus(tokens, asLval, statementStart);
		Token tok = tokens.Peek();
		while (tok.type == Token.Type.OpTimes || tok.type == Token.Type.OpDivide || tok.type == Token.Type.OpMod) {
			tokens.Dequeue();

			AllowLineBreak(tokens); // allow a line break after a binary operator

			val = FullyEvaluate(val);
			Value opB = ParseUnaryMinus(tokens);
			int tempNum = output.nextTempNum++;
			switch (tok.type) {
				case OpTimes:
					output.Add(new Line(TAC.LTemp(tempNum), Line.Op.ATimesB, val, opB));
					break;
				case OpDivide:
					output.Add(new Line(TAC.LTemp(tempNum), Line.Op.ADividedByB, val, opB));
					break;
				case OpMod:
					output.Add(new Line(TAC.LTemp(tempNum), Line.Op.AModB, val, opB));
					break;
				default:
					// keep compiler happy
					break;
			}
			val = TAC.RTemp(tempNum);

			tok = tokens.Peek();
		}
		return val;
	}
	
	Value ParseUnaryMinus(Lexer tokens) throws LexerException, CompilerException {
		return ParseUnaryMinus(tokens, false, false);
	}
	
	Value ParseUnaryMinus(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseUnaryMinus(tokens, asLval, false);
	}
		
	Value ParseUnaryMinus(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseNew;
		if (tokens.Peek().type != Token.Type.OpMinus) return ParseNew(tokens, asLval, statementStart);
		tokens.Dequeue();		// skip '-'

		AllowLineBreak(tokens); // allow a line break after a unary operator

		Value val = ParseNew(tokens);
		if (val instanceof ValNumber) {
			// If what follows is a numeric literal, just invert it and be done!
			ValNumber valnum = (ValNumber)val;
			valnum.value = -valnum.value;
			return valnum;
		}
		// Otherwise, subtract it from 0 and return a new temporary.
		int tempNum = output.nextTempNum++;
		output.Add(new Line(TAC.LTemp(tempNum), Line.Op.AMinusB, TAC.Num(0), val));

		return TAC.RTemp(tempNum);
	}
	
	Value ParseNew(Lexer tokens) throws LexerException, CompilerException {
		return ParseNew(tokens, false, false);
	}
	
	Value ParseNew(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseNew(tokens, asLval, false);
	}

	Value ParseNew(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseAddressOf;
		if (tokens.Peek().type != Token.Type.Keyword || tokens.Peek().text != "new") return ParseAddressOf(tokens, asLval, statementStart);
		tokens.Dequeue();		// skip 'new'

		AllowLineBreak(tokens); // allow a line break after a unary operator

		// Grab a reference to our __isa value
		Value isa = ParseAddressOf(tokens);
		// Now, create a new map, and set __isa on it to that.
		// NOTE: we must be sure this map gets created at runtime, not here at parse time.
		// Since it is a mutable object, we need to return a different one each time
		// this code executes (in a loop, function, etc.).  So, we use Op.CopyA below!
		ValMap map = new ValMap();
		map.SetElem(ValString.magicIsA, isa);
		Value result = new ValTemp(output.nextTempNum++);
		output.Add(new Line(result, Line.Op.CopyA, map));
		return result;
	}
	
	Value ParseAddressOf(Lexer tokens) throws LexerException, CompilerException {
		return ParseAddressOf(tokens, false, false);
	}
	
	Value ParseAddressOf(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseAddressOf(tokens, asLval, false);
	}

	Value ParseAddressOf(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParsePower;
		if (tokens.Peek().type != Token.Type.AddressOf) return ParsePower(tokens, asLval, statementStart);
		tokens.Dequeue();
		AllowLineBreak(tokens); // allow a line break after a unary operator
		Value val = ParsePower(tokens, true, statementStart);
		if (val instanceof ValVar) {
			((ValVar)val).noInvoke = true;
		} else if (val instanceof ValSeqElem) {
			((ValSeqElem)val).noInvoke = true;
		}
		return val;
	}
	
	Value ParsePower(Lexer tokens) throws LexerException, CompilerException {
		return ParsePower(tokens, false, false);
	}
	
	Value ParsePower(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParsePower(tokens, asLval, false);
	}

	Value ParsePower(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseCallExpr;
		Value val = ParseCallExpr(tokens, asLval, statementStart);
		Token tok = tokens.Peek();
		while (tok.type == Token.Type.OpPower) {
			tokens.Dequeue();

			AllowLineBreak(tokens); // allow a line break after a binary operator

			val = FullyEvaluate(val);
			Value opB = ParseCallExpr(tokens);
			int tempNum = output.nextTempNum++;
			output.Add(new Line(TAC.LTemp(tempNum), Line.Op.APowB, val, opB));
			val = TAC.RTemp(tempNum);

			tok = tokens.Peek();
		}
		return val;
	}


	Value FullyEvaluate(Value val) {
		if (val instanceof ValVar) {
			ValVar var = (ValVar)val;
			// If var was protected with @, then return it as-is; don't attempt to call it.
			if (var.noInvoke) return val;
			// Don't invoke super; leave as-is so we can do special handling
			// of it at runtime.  Also, as an optimization, same for "self".
			if (var.identifier == "super" || var.identifier == "self") return val;
			// Evaluate a variable (which might be a function we need to call).				
			ValTemp temp = new ValTemp(output.nextTempNum++);
			output.Add(new Line(temp, Line.Op.CallFunctionA, val, ValNumber.zero));
			return temp;
		} else if (val instanceof ValSeqElem) {
			ValSeqElem elem = ((ValSeqElem)val);
			// If sequence element was protected with @, then return it as-is; don't attempt to call it.
			if (elem.noInvoke) return val;
			// Evaluate a sequence lookup (which might be a function we need to call).				
			ValTemp temp = new ValTemp(output.nextTempNum++);
			output.Add(new Line(temp, Line.Op.CallFunctionA, val, ValNumber.zero));
			return temp;
		}
		return val;
	}
	
	Value ParseCallExpr(Lexer tokens) throws LexerException, CompilerException {
		return ParseCallExpr(tokens, false, false);
	}
	
	
	Value ParseCallExpr(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseCallExpr(tokens, asLval, false);
	}
	
	Value ParseCallExpr(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseMap;
		Value val = ParseMap(tokens, asLval, statementStart);
		while (true) {
			if (tokens.Peek().type == Token.Type.Dot) {
				tokens.Dequeue();	// discard '.'
				AllowLineBreak(tokens); // allow a line break after a binary operator
				Token nextIdent = RequireToken(tokens, Token.Type.Identifier);
				// We're chaining sequences here; look up (by invoking)
				// the previous part of the sequence, so we can build on it.
				val = FullyEvaluate(val);
				// Now build the lookup.
				val = new ValSeqElem(val, new ValString(nextIdent.text));
				if (tokens.Peek().type == Token.Type.LParen && !tokens.Peek().afterSpace) {
					// If this new element is followed by parens, we need to
					// parse it as a call right away.
					val = ParseCallArgs(val, tokens);
					//val = FullyEvaluate(val);
				}				
			} else if (tokens.Peek().type == Token.Type.LSquare && !tokens.Peek().afterSpace) {
				tokens.Dequeue();	// discard '['
				AllowLineBreak(tokens); // allow a line break after open bracket
				val = FullyEvaluate(val);

				if (tokens.Peek().type == Token.Type.Colon) {	// e.g., foo[:4]
					tokens.Dequeue();	// discard ':'
					AllowLineBreak(tokens); // allow a line break after colon
					Value index2 = null;
					if (tokens.Peek().type != Token.Type.RSquare) index2 = ParseExpr(tokens);
					ValTemp temp = new ValTemp(output.nextTempNum++);
					Intrinsics.CompileSlice(output.code, val, null, index2, temp.tempNum);
					val = temp;
				} else {
					Value index = ParseExpr(tokens);
					if (tokens.Peek().type == Token.Type.Colon) {	// e.g., foo[2:4] or foo[2:]
						tokens.Dequeue();	// discard ':'
						AllowLineBreak(tokens); // allow a line break after colon
						Value index2 = null;
						if (tokens.Peek().type != Token.Type.RSquare) index2 = ParseExpr(tokens);
						ValTemp temp = new ValTemp(output.nextTempNum++);
						Intrinsics.CompileSlice(output.code, val, index, index2, temp.tempNum);
						val = temp;
					} else {			// e.g., foo[3]  (not a slice at all)
						if (statementStart) {
							// At the start of a statement, we don't want to compile the
							// last sequence lookup, because we might have to convert it into
							// an assignment.  But we want to compile any previous one.
							if (val instanceof ValSeqElem) {
								ValSeqElem vsVal = (ValSeqElem)val;
								ValTemp temp = new ValTemp(output.nextTempNum++);
								output.Add(new Line(temp, Line.Op.ElemBofA, vsVal.sequence, vsVal.index));
								val = temp;
							}
							val = new ValSeqElem(val, index);
						} else {
							// Anywhere else in an expression, we can compile the lookup right away.
							ValTemp temp = new ValTemp(output.nextTempNum++);
							output.Add(new Line(temp, Line.Op.ElemBofA, val, index));
							val = temp;
						}
					}
				}

				RequireToken(tokens, Token.Type.RSquare);
			} else if ((val instanceof ValVar && !((ValVar)val).noInvoke) || val instanceof ValSeqElem) {
				// Got a variable... it might refer to a function!
				if (!asLval || (tokens.Peek().type == Token.Type.LParen && !tokens.Peek().afterSpace)) {
					// If followed by parens, definitely a function call, possibly with arguments!
					// If not, well, let's call it anyway unless we need an lvalue.
					val = ParseCallArgs(val, tokens);
				} else break;
			} else break;
		}
		
		return val;
	}
	
	Value ParseMap(Lexer tokens) throws LexerException, CompilerException {
		return ParseMap(tokens, false, false);
	}
	
	Value ParseMap(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseMap(tokens, asLval, false);
	}

	Value ParseMap(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseList;
		if (tokens.Peek().type != Token.Type.LCurly) return ParseList(tokens, asLval, statementStart);
		tokens.Dequeue();
		// NOTE: we must be sure this map gets created at runtime, not here at parse time.
		// Since it is a mutable object, we need to return a different one each time
		// this code executes (in a loop, function, etc.).  So, we use Op.CopyA below!
		ValMap map = new ValMap();
		if (tokens.Peek().type == Token.Type.RCurly) {
			tokens.Dequeue();
		} else while (true) {
			AllowLineBreak(tokens); // allow a line break after a comma or open brace

			// Allow the map to close with a } on its own line. 
			if (tokens.Peek().type == Token.Type.RCurly) {
				tokens.Dequeue();
				break;
			}

			Value key = ParseExpr(tokens);
			RequireToken(tokens, Token.Type.Colon);
			AllowLineBreak(tokens); // allow a line break after a colon
			Value value = ParseExpr(tokens);
			map.map.put(key == null ? ValNull.instance : key, value);
			
			if (RequireEitherToken(tokens, Token.Type.Comma, Token.Type.RCurly).type == Token.Type.RCurly) break;
		}
		Value result = new ValTemp(output.nextTempNum++);
		output.Add(new Line(result, Line.Op.CopyA, map));
		return result;
	}
	
	Value ParseList(Lexer tokens) throws LexerException, CompilerException {
		return ParseList(tokens, false, false);
	}
	
	Value ParseList(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseList(tokens, asLval, false);
	}

	//		list	:= '[' expr [, expr, ...] ']'
	//				 | quantity
	Value ParseList(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseQuantity;
		if (tokens.Peek().type != Token.Type.LSquare) return ParseQuantity(tokens, asLval, statementStart);
		tokens.Dequeue();
		// NOTE: we must be sure this list gets created at runtime, not here at parse time.
		// Since it is a mutable object, we need to return a different one each time
		// this code executes (in a loop, function, etc.).  So, we use Op.CopyA below!
		ValList list = new ValList();
		if (tokens.Peek().type == Token.Type.RSquare) {
			tokens.Dequeue();
		} else while (true) {
			AllowLineBreak(tokens); // allow a line break after a comma or open bracket

			// Allow the list to close with a ] on its own line. 
			if (tokens.Peek().type == Token.Type.RSquare) {
				tokens.Dequeue();
				break;
			}

			Value elem = ParseExpr(tokens);
			list.values.add(elem);
			if (RequireEitherToken(tokens, Token.Type.Comma, Token.Type.RSquare).type == Token.Type.RSquare) break;
		}
		if (statementStart) return list;	// return the list as-is for indexed assignment (foo[3]=42)
		Value result = new ValTemp(output.nextTempNum++);
		output.Add(new Line(result, Line.Op.CopyA, list));	// use COPY on this mutable list!
		return result;
	}
	
	Value ParseQuantity(Lexer tokens) throws LexerException, CompilerException {
		return ParseQuantity(tokens, false, false);
	}
	
	Value ParseQuantity(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseQuantity(tokens, asLval, false);
	}

	//		quantity := '(' expr ')'
	//				  | call
	Value ParseQuantity(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		//ExpressionParsingMethod nextLevel = this::ParseAtom;
		if (tokens.Peek().type != Token.Type.LParen) return ParseAtom(tokens, asLval, statementStart);
		tokens.Dequeue();
		AllowLineBreak(tokens); // allow a line break after an open paren
		Value val = ParseExpr(tokens);
		RequireToken(tokens, Token.Type.RParen);
		return val;
	}

	/// <summary>
	/// Helper method that gathers arguments, emitting SetParamAasB for each one,
	/// and then emits the actual call to the given function.  It works both for
	/// a parenthesized set of arguments, and for no parens (i.e. no arguments).
	/// </summary>
	/// <returns>The call arguments.</returns>
	/// <param name="funcRef">Function to invoke.</param>
	/// <param name="tokens">Token stream.</param>
	Value ParseCallArgs(Value funcRef, Lexer tokens) throws LexerException, CompilerException {
		int argCount = 0;
		if (tokens.Peek().type == Token.Type.LParen) {
			tokens.Dequeue();		// remove '('
			if (tokens.Peek().type == Token.Type.RParen) {
				tokens.Dequeue();
			} else while (true) {
				AllowLineBreak(tokens); // allow a line break after a comma or open paren
				Value arg = ParseExpr(tokens);
				output.Add(new Line(null, Line.Op.PushParam, arg));
				argCount++;
				if (RequireEitherToken(tokens, Token.Type.Comma, Token.Type.RParen).type == Token.Type.RParen) break;
			}
		}
		ValTemp result = new ValTemp(output.nextTempNum++);
		output.Add(new Line(result, Line.Op.CallFunctionA, funcRef, TAC.Num(argCount)));
		return result;
	}
	
	Value ParseAtom(Lexer tokens) throws LexerException, CompilerException {
		return ParseAtom(tokens, false, false);
	}
	
	Value ParseAtom(Lexer tokens, boolean asLval) throws LexerException, CompilerException {
		return ParseAtom(tokens, asLval, false);
	}
		
	Value ParseAtom(Lexer tokens, boolean asLval, boolean statementStart) throws LexerException, CompilerException {
		Token tok = !tokens.AtEnd() ? tokens.Dequeue() : Token.EOL;
		if (tok.type == Token.Type.Number) {
			try {
				return new ValNumber(Double.parseDouble(tok.text));
			} catch (NumberFormatException e) {
				throw new CompilerException("invalid numeric literal: " + tok.text);
			}
		} else if (tok.type == Token.Type.String) {
			return new ValString(tok.text);
		} else if (tok.type == Token.Type.Identifier) {
			if (tok.text == "self") return ValVar.self;
			return new ValVar(tok.text);
		} else if (tok.type == Token.Type.Keyword) {
			switch (tok.text) {
			case "null":	return null;
			case "true":	return ValNumber.one;
			case "false":	return ValNumber.zero;
			}
		}
		throw new CompilerException(String.format("got %s where number, string, or identifier is required", tok));
	}

	Token RequireToken(Lexer tokens, Token.Type type) throws LexerException, CompilerException {
		return RequireToken(tokens, type, null);
	}

	/// <summary>
	/// The given token type and text is required. So, consume the next token,
	/// and if it doesn't match, throw an error.
	/// </summary>
	/// <param name="tokens">Token queue.</param>
	/// <param name="type">Required token type.</param>
	/// <param name="text">Required token text (if applicable).</param>
	Token RequireToken(Lexer tokens, Token.Type type, String text) throws LexerException, CompilerException {
		Token got = (tokens.AtEnd() ? Token.EOL : tokens.Dequeue());
		if (got.type != type || (text != null && got.text != text)) {
			Token expected = new Token(type, text);
			throw new CompilerException(errorContext, tokens.lineNum,  String.format("got %s where %s is required", got, expected));
		}
		return got;
	}
	
	Token RequireEitherToken(Lexer tokens, Token.Type type1, String text1, Token.Type type2) throws LexerException, CompilerException {
		return RequireEitherToken(tokens, type1, text1, type2, null);
	}

	Token RequireEitherToken(Lexer tokens, Token.Type type1, String text1, Token.Type type2, String text2) throws LexerException, CompilerException {
		Token got = (tokens.AtEnd() ? Token.EOL : tokens.Dequeue());
		if ((got.type != type1 && got.type != type2)
			|| ((text1 != null && got.text != text1) && (text2 != null && got.text != text2))) {
			Token expected1 = new Token(type1, text1);
			Token expected2 = new Token(type2, text2);
			throw new CompilerException(errorContext, tokens.lineNum, String.format("got %s where %s or %s is required", got, expected1, expected2));
		}
		return got;
	}
	
	Token RequireEitherToken(Lexer tokens, Token.Type type1, Token.Type type2) throws LexerException, CompilerException {
		return RequireEitherToken(tokens, type1, type2, null);
	}

	Token RequireEitherToken(Lexer tokens, Token.Type type1, Token.Type type2, String text2) throws LexerException, CompilerException {
		return RequireEitherToken(tokens, type1, null, type2, text2);
	}
	
	static void TestValidParse(String src) {
		TestValidParse(src, false);
	}

	static void TestValidParse(String src, boolean dumpTac) {
	Parser parser = new Parser();
		try {
			parser.Parse(src);
		} catch (Exception e) {
			System.out.println(e.toString() + " while parsing:");
			System.out.println(src);
		}
		if (dumpTac && parser.output != null) TAC.Dump(parser.output.code, -1);
	}

	public static void RunUnitTests() {
		TestValidParse("pi < 4");
		TestValidParse("(pi < 4)");
		TestValidParse("if true then 20 else 30");
		TestValidParse("f = function(x)\nreturn x*3\nend function\nf(14)");
		TestValidParse("foo=\"bar\"\nindexes(foo*2)\nfoo.indexes");
		TestValidParse("x=[]\nx.push(42)");
		TestValidParse("list1=[10, 20, 30, 40, 50]; range(0, list1.len)");
		TestValidParse("f = function(x); print(\"foo\"); end function; print(false and f)");
		TestValidParse("print 42");
		TestValidParse("print true");
		TestValidParse("f = function(x)\nprint x\nend function\nf 42");
		TestValidParse("myList = [1, null, 3]");
		TestValidParse("while true; if true then; break; else; print 1; end if; end while");
		TestValidParse("x = 0 or\n1");
		TestValidParse("x = [1, 2, \n 3]");
		TestValidParse("range 1,\n10, 2");
	}
}
