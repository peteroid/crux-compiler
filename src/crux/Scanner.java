package crux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class Scanner implements Iterable<Token> {
	public static String studentName = "TODO: YOUR NAME";
	public static String studentID = "TODO: Your 8-digit id";
	public static String uciNetID = "TODO: uci-net id";
	
	private int lineNum;  // current line count
	private int charPos;  // character offset for current line
	private int nextChar; // contains the next char (-1 == EOF)
	private Reader input, readInput;

	private String stringBuffer;

	private enum State {
		START,
		END,
		SPECIAL_CHAR,
		KEYWORD,
		IDENTIFIER,
		INTEGER,
		FLOAT
	}
	
	Scanner(Reader reader)
	{
		// TODO: initialize the Scanner
		input = reader;
		readInput = new BufferedReader(input);
		lineNum = 1;
		charPos = 1;
		stringBuffer = "";
		nextChar = readChar();
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
//			System.out.println(String.valueOf(c) + " detected!");
			} catch (IOException e) {
				e.printStackTrace();
//			System.out.print(String.valueOf(c) + " caused error!");
			}
		}
		return c == 65535? -1 : c;
	}

	/* Invariants:
	 *  1. call assumes that nextChar is already holding an unread character
	 *  2. return leaves nextChar containing an untokenized character
	 */
	public Token next()
	{
		// TODO: implement this
		State currentState, lastState = null;
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
						if (isDot(nextChar)) {
							nextStates.add(State.FLOAT);
						} else if (isDigit(nextChar)) {
							nextStates.add(State.INTEGER);
						} else {
							currentStates.add(State.END);
						}
						break;
					case FLOAT:
						if (isDigit(nextChar)) {
							nextStates.add(State.FLOAT);
						} else {
							currentStates.add(State.END);
						}
						break;
					case KEYWORD:
						if (isLetter(nextChar))
							if (Token.Kind.matchesWithKeywords(readString + (char) nextChar))
								nextStates.add(State.END);
							else if (Token.Kind.startsWithKeywords(readString + (char) nextChar))
								nextStates.add(State.KEYWORD);
							else
								nextStates.add(State.IDENTIFIER);
						else if (isIdentifierLexeme(nextChar))
							nextStates.add(State.IDENTIFIER);
						else
							nextStates.add(State.END);
						break;
					case IDENTIFIER:
						if (isIdentifierLexeme(nextChar))
							nextStates.add(State.IDENTIFIER);
						else
							currentStates.add(State.END);
						break;
					case SPECIAL_CHAR:
						if (Token.Kind.matchesWithSpecialChars(readString + (char) nextChar))
							nextStates.add(State.END);
						else
							currentStates.add(State.END);
						break;
					case END:
						int lastChar = nextChar;

						// get the matched token
						String lexeme = readString;
						Token t = null;

						if (lexeme.isEmpty()) {
							if (nextChar == (int) '\n' || nextChar == (int) ' ') {
								nextStates.add(State.START);
							} else if (nextChar == -1) {
								t = Token.EOF(lineNum, charPos);
							}
						} else {
							if (Token.Kind.matches(lexeme)) {
								t = new Token(lexeme, lineNum, charPos);
							} else if (lexeme.indexOf('.') != -1) {
								t = Token.Float(lexeme, lineNum, charPos);
							} else if (isDigit(lexeme.charAt(0))) {
								t = Token.Integer(lexeme, lineNum, charPos);
							}  else if (isIdentifier(lexeme)) {
								t = Token.Identifier(lexeme, lineNum, charPos);
							} else {
								t = new Token(lexeme, lineNum, charPos);
							}
						}

						// determine the lastMatch
						if ((t != null) && (lastMatch == null || !t.is(Token.Kind.ERROR))) {
							lastMatch = t;
						}
						break;
					default:
						System.out.println("Unexpected state");
						break;
				}
			}

			if (readString.isEmpty()) {
				if (nextChar == (int) '\n') {
					lineNum++;
					charPos = 1;
				} else if (nextChar == ' ') {
					charPos++;
				} else {
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
	public Iterator<Token> iterator() {
		return null;
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
		return isDigit(c) || c == (int) '_' || isLetter(c);
	}

	public static boolean isIdentifier(String s) {
		if (!s.isEmpty() && (s.charAt(0) == (int) '_' || isLetter(s.charAt(0)))) {
			for (char c : s.toCharArray())
				if (!isIdentifierLexeme(c))
					return false;
			return true;
		} else {
			return false;
		}
	}
}
