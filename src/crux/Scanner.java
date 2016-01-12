package crux;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class Scanner implements Iterable<Token> {
	public static String studentName = "TODO: YOUR NAME";
	public static String studentID = "TODO: Your 8-digit id";
	public static String uciNetID = "TODO: uci-net id";
	
	private int lineNum;  // current line count
	private int charPos;  // character offset for current line
	private int nextChar; // contains the next char (-1 == EOF)
	private Reader input;
	
	Scanner(Reader reader)
	{
		// TODO: initialize the Scanner
		input = reader;
		lineNum = 1;
		charPos = 1;
		nextChar = readChar();
	}	
	
	// OPTIONAL: helper function for reading a single char from input
	//           can be used to catch and handle any IOExceptions,
	//           advance the charPos or lineNum, etc.

	private int readChar() {
		int c = -2;
		try {
			c = input.read();
			System.out.println(String.valueOf(c) + " detected!");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.print(String.valueOf(c) + " caused error!");
		}
		return c;
	}



	/* Invariants:
	 *  1. call assumes that nextChar is already holding an unread character
	 *  2. return leaves nextChar containing an untokenized character
	 */
	public Token next()
	{
		// TODO: implement this
		Token t;
		switch (nextChar) {
			case -1:
				t = Token.EOF(lineNum, charPos);
				break;
			case ' ':
				charPos++;
				nextChar = readChar();
				t = next();
				break;
			case '\n':
				lineNum++;
				charPos = 1;
				nextChar = readChar();
				t = next();
				break;
			default:
				String read = String.valueOf((char) nextChar);
				int charCount = 0;
				while (Token.Kind.matches(read)) {
					charCount++;
					nextChar = readChar();
					read += String.valueOf((char) nextChar);
				}
				t = new Token(read.substring(0, charCount), lineNum, charPos);
				charPos += charCount;
				break;
		}
		return t;
	}

	@Override
	public Iterator<Token> iterator() {
		return null;
	}

	// OPTIONAL: any other methods that you find convenient for implementation or testing
}
