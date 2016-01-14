package crux;

public class Token {
	
	public enum Kind {
		AND("and"),
		OR("or"),
		NOT("not"),
		
		ADD("+"),
		SUB("-"),
		MUL("*"),
		DIV("/"),
		
		// TODO: complete the list of possible tokens
		LET("let"),
		VAR("var"),
		ARRAY("array"),
		FUNC("func"),
		IF("if"),
		ELSE("else"),
		WHILE("while"),
		TRUE("true"),
		FALSE("false"),
		RETURN("return"),

		IDENTIFIER(),
		INTEGER(),
		FLOAT(),
		ERROR(),
		EOF(),

		OPEN_PAREN("("),
		CLOSE_PAREN(")"),
		OPEN_BRACE("{"),
		CLOSE_BRACE("}"),
		OPEN_BRACKET("["),
		CLOSE_BRACKET("]"),
		GREATER_EQUAL(">="),
		LESSER_EQUAL("<="),
		NOT_EQUAL("!="),
		EQUAL("=="),
		GREATER_THAN(">"),
		LESS_THAN("<"),
		ASSIGN("="),
		COMMA(","),
		SEMICOLON(";"),
		COLON(":"),
		CALL("::");

		public final static Kind[] KEYWORDS =
				{AND, OR, NOT, LET, VAR, ARRAY, FUNC, IF, ELSE, WHILE, TRUE, FALSE, RETURN};

		public final static Kind[] SPECIAL_CHARS =
				{OPEN_PAREN, CLOSE_PAREN, OPEN_BRACE, CLOSE_BRACE, OPEN_BRACKET, CLOSE_BRACKET,
				ADD, SUB, MUL, DIV, GREATER_EQUAL, LESSER_EQUAL, NOT_EQUAL, EQUAL, GREATER_THAN,
				LESS_THAN, ASSIGN, COMMA, SEMICOLON, COLON, CALL};

		private String default_lexeme;
		
		Kind()
		{
			default_lexeme = "";
		}
		
		Kind(String lexeme)
		{
			default_lexeme = lexeme;
		}
		
		public boolean hasStaticLexeme()
		{
			return default_lexeme != null;
		}
		
		// OPTIONAL: if you wish to also make convenience functions, feel free
		//           for example, boolean matches(String lexeme)
		//           can report whether a Token.Kind has the given lexeme

		static boolean matches(String lexeme) {
			return matchesWithSpecialChars(lexeme) || matchesWithKeywords(lexeme);
		}

		private static boolean matchesWithKinds(Kind[] kinds, String s) {
			for (Kind kind : kinds) {
				if (kind.default_lexeme.equals(s))
					return true;
			}
			return false;
		}

		static boolean matchesWithSpecialChars(String s) {
			return matchesWithKinds(SPECIAL_CHARS, s);
		}

		static boolean matchesWithKeywords(String s) {
			return matchesWithKinds(KEYWORDS, s);
		}

		private static boolean startsWithKinds(Kind[] kinds, String s) {
			for (Kind kind : kinds) {
				if (kind.default_lexeme.startsWith(s)) {
					return true;
				}
			}
			return false;
		}

		static boolean startsWithSpecialChars(String s) {
			return startsWithKinds(SPECIAL_CHARS, s);
		}

		static boolean startsWithKeywords(String s) {
			return startsWithKinds(KEYWORDS, s);
		}
	}
	
	private int lineNum;
	private int charPos;
	Kind kind;
	private String lexeme = "";
	
	
	// OPTIONAL: implement factory functions for some tokens, as you see fit
	public static Token EOF(int linePos, int charPos)
	{
		Token tok = new Token(linePos, charPos);
		tok.kind = Kind.EOF;
		return tok;
	}

	public static Token Identifier(String name, int lineNum, int charPos){
		Token t = new Token(lineNum, charPos);
		t.kind = Kind.IDENTIFIER;
		t.lexeme = name;
		return t;
	}

	public static Token Integer(String name, int lineNum, int charPos){
		Token t = new Token(lineNum, charPos);
		t.kind = Kind.INTEGER;
		t.lexeme = name;
		return t;
	}

	public static Token Float(String name, int lineNum, int charPos){
		Token t = new Token(lineNum, charPos);
		t.kind = Kind.FLOAT;
		t.lexeme = name;
		return t;
	}

	private Token(int lineNum, int charPos)
	{
		this.lineNum = lineNum;
		this.charPos = charPos;
		
		// if we don't match anything, signal error
		this.kind = Kind.ERROR;
		this.lexeme = "No Lexeme Given";
	}
	
	public Token(String lexeme, int lineNum, int charPos)
	{
		this.lineNum = lineNum;
		this.charPos = charPos;
		
		// TODO: based on the given lexeme determine and set the actual kind
		for (Kind kind : Kind.values()) {
			if (kind.default_lexeme.equals(lexeme)){
				this.kind = kind;
				this.lexeme = lexeme;
				return;
			}
		}
		
		// if we don't match anything, signal error
		this.kind = Kind.ERROR;
		this.lexeme = lexeme;
	}
	
	public int lineNumber()
	{
		return lineNum;
	}
	
	public int charPosition()
	{
		return charPos;
	}
	
	// Return the lexeme representing or held by this token
	public String lexeme()
	{
		// TODO: implement
		return this.lexeme;
	}
	
	public String toString()
	{
		// TODO: implement this
		String str = this.kind.name();
		switch (this.kind) {
			case ERROR:
				str += "(Unexpected character: " + this.lexeme() + ")";
				break;
			case INTEGER:
			case FLOAT:
				str += "(" + this.lexeme() + ")";
				break;
			case IDENTIFIER:
				str += "(" + this.lexeme() + ")";
				break;
			default:
				break;
		}
		str += "(lineNum:" + String.valueOf(this.lineNum) +
				", charPos:" + String.valueOf(this.charPos) + ")";
		return  str;
	}
	
	// OPTIONAL: function to query a token about its kind
	public boolean is(Token.Kind kind) {
		return kind.equals(this.kind);
	}
	
	// OPTIONAL: add any additional helper or convenience methods
	//           that you find make for a clean design
}
