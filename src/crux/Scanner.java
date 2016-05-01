package crux;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;

public class Scanner implements Iterable<Token>, Iterator<Token> {

	public static String studentName = "TODO: YOUR NAME";
	public static String studentID = "TODO: Your 8-digit id";
	public static String uciNetID = "TODO: uci-net id";
	
	private int lineNum;  // current line count
	private int charPos;  // character offset for current line
	private int nextChar; // contains the next char (-1 == EOF)
	private Reader input;

	// stringBuffer is used to store the extra characters read by the programme
	// this is used to reset the reader
	private String stringBuffer;
	private boolean isReachedEOF;

	private enum State {
		START,
		END,
		SPECIAL_CHAR,
		KEYWORD,
		IDENTIFIER,
		INTEGER,
		FLOAT,
		COMMENT
	}
	
	Scanner(Reader reader) {
		// initialize the Scanner
		input = reader;
		lineNum = 1;
		charPos = 1;
		stringBuffer = "";
		nextChar = readChar();
		isReachedEOF = false;
	}
	
	// OPTIONAL: helper function for reading a single char from input
	//           can be used to catch and handle any IOExceptions,
	//           advance the charPos or lineNum, etc.

	private int readChar() {
		int c = -2;
		if (!stringBuffer.isEmpty()) {
			c = stringBuffer.charAt(0);
			stringBuffer = stringBuffer.substring(1);
		} else {
			try {
				c = input.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return c == 65535? -1 : c;
	}

	@Override
	public boolean hasNext() {
		return !isReachedEOF;
	}

	/* Invariants:
     *  1. call assumes that nextChar is already holding an unread character
     *  2. return leaves nextChar containing an untokenized character
     */
	public Token next()
	{
		State currentState;
		LinkedList<State> nextStates = new LinkedList<State>();
		nextStates.add(State.START);

		String readString = "";
		Token lastMatch = null;
		do {
			LinkedList<State> currentStates = new LinkedList<State>(nextStates);
			nextStates.clear();
			while (!currentStates.isEmpty()) {
				currentState = currentStates.pop();
				switch (currentState) {
					case START:
						if (isDigit(nextChar)) {
							nextStates.add(State.INTEGER);
						} else if (isLetter(nextChar) || nextChar == '_') {
							if (Token.Kind.startsWithKeywords(String.valueOf((char) nextChar)))
								nextStates.add(State.KEYWORD);
							else
								nextStates.add(State.IDENTIFIER);
						} else if (Token.Kind.startsWithSpecialChars(String.valueOf((char) nextChar))) {
							nextStates.add(State.SPECIAL_CHAR);
							if (Token.Kind.matchesWithSpecialChars(String.valueOf((char) nextChar)))
								nextStates.add(State.END);
						} else {
							currentStates.add(State.END);
						}
						break;
					case INTEGER:
						if (isDot(nextChar))
							nextStates.add(State.FLOAT);
						else if (isDigit(nextChar))
							nextStates.add(State.INTEGER);
						else
							currentStates.add(State.END);
						break;
					case FLOAT:
						if (isDigit(nextChar))
							nextStates.add(State.FLOAT);
						else
							currentStates.add(State.END);
						break;
					case KEYWORD:
						if (isLetter(nextChar))
							if (Token.Kind.matchesWithKeywords(readString + (char) nextChar)) {
								nextStates.add(State.END);
								nextStates.add(State.IDENTIFIER);
							} else if (Token.Kind.startsWithKeywords(readString + (char) nextChar)) {
								nextStates.add(State.KEYWORD);
							} else {
								nextStates.add(State.IDENTIFIER);
							}
						else if (isIdentifierLexeme(nextChar))
							nextStates.add(State.IDENTIFIER);
						else
							currentStates.add(State.END);
						break;
					case IDENTIFIER:
						if (isIdentifierLexeme(nextChar))
							nextStates.add(State.IDENTIFIER);
						else
							currentStates.add(State.END);
						break;
					case SPECIAL_CHAR:
						if ((readString + (char) nextChar).equals("//")) {
							currentStates.clear();
							nextStates.clear();
							currentStates.add(State.COMMENT);
						} else if (Token.Kind.matchesWithSpecialChars(readString + (char) nextChar)) {
							nextStates.add(State.END);
						} else {
							currentStates.add(State.END);
						}
						break;
					case COMMENT:
						// ignore the char until the line break
						charPos++;
						while (nextChar != '\n' && nextChar != -1) {
							nextChar = readChar();
							charPos++;
						}
						readString = "";
						nextStates.add(State.START);
						break;
					case END:

						// get the matched token
						String lexeme = readString;
						Token t = null;

						if (lexeme.isEmpty()) {
							if (nextChar == '\n' || nextChar == ' ' || nextChar == '\t') {
								nextStates.add(State.START);
							} else if (nextChar == -1) {
								t = Token.EOF(lineNum, charPos);
								isReachedEOF = true;
							} else {
								t = new Token(String.valueOf((char) nextChar), lineNum, charPos);
							}
						} else {
							if (Token.Kind.matches(lexeme))
								t = new Token(lexeme, lineNum, charPos);
							else if (lexeme.indexOf('.') != -1)
								t = Token.Float(lexeme, lineNum, charPos);
							else if (isDigit(lexeme.charAt(0)))
								t = Token.Integer(lexeme, lineNum, charPos);
							else if (isIdentifier(lexeme))
								t = Token.Identifier(lexeme, lineNum, charPos);
							else
								t = new Token(lexeme, lineNum, charPos);
						}

						// determine the lastMatch
						if ((t != null) && (lastMatch == null || !t.is(Token.Kind.ERROR)))
							lastMatch = t;
						break;
					default:
						System.out.println("Unexpected state");
						break;
				}
			}

			if (readString.isEmpty()) {
				if (nextChar == '\n') {
					lineNum++;
					charPos = 1;
				} else if (nextChar == ' ' || nextChar == '\t') {
					charPos++;
				} else if (nextChar != -1) {
					readString += String.valueOf((char) nextChar);
				}
			} else {
				readString += String.valueOf((char) nextChar);
			}

			if (!nextStates.isEmpty())
				nextChar = readChar();
		} while (!nextStates.isEmpty());

		if (readString.length() > lastMatch.lexeme().length())
			stringBuffer = readString.substring(lastMatch.lexeme().length());

		nextChar = readChar();
		charPos += lastMatch.lexeme().length();
		return lastMatch;
	}

	@Override
	public void remove() {
		// not supported, not going to implement
	}

	@Override
	public Iterator<Token> iterator() {
		return this;
	}

	// OPTIONAL: any other methods that you find convenient for implementation or testing
	public static boolean isDigit(int c) {
		return 48 <= c && c <= 57;
	}

	public static boolean isDot(int c) {
		return c == 46;
	}

	public static boolean isUppercaseLetter(int c) {
		return 65 <= c && c <= 90;
	}

	public static boolean isLowercaseLetter(int c) {
		return 97 <= c && c <= 122;
	}

	public static boolean isLetter(int c) {
		return isLowercaseLetter(c) || isUppercaseLetter(c);
	}

	public static boolean isIdentifierLexeme(int c) {
		return isDigit(c) || c == '_' || isLetter(c);
	}

	public static boolean isIdentifier(String s) {
		if (!s.isEmpty() && (s.charAt(0) == '_' || isLetter(s.charAt(0)))) {
			for (char c : s.toCharArray())
				if (!isIdentifierLexeme(c))
					return false;
			return true;
		} else {
			return false;
		}
	}
}
