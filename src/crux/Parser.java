package crux;

import ast.Command;
import ast.Expression;

public class Parser {
    public static String studentName = "TODO: Your Name";
    public static String studentID = "TODO: Your 8-digit id";
    public static String uciNetID = "TODO: uci-net id";

// Grammar Rule Reporting ==========================================
    private int parseTreeRecursionDepth = 0;
    private StringBuffer parseTreeBuffer = new StringBuffer();

    public void enterRule(NonTerminal nonTerminal) {
        String lineData = new String();
        for(int i = 0; i < parseTreeRecursionDepth; i++)
        {
            lineData += "  ";
        }
        lineData += nonTerminal.name();
        //System.out.println("descending " + lineData);
        parseTreeBuffer.append(lineData + "\n");
        parseTreeRecursionDepth++;
    }

    private void exitRule(NonTerminal nonTerminal)
    {
        parseTreeRecursionDepth--;
    }

    public String parseTreeReport()
    {
        return parseTreeBuffer.toString();
    }


// Error Reporting ==========================================
    private StringBuffer errorBuffer = new StringBuffer();
    
    private String reportSyntaxError(NonTerminal nt)
    {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected a token from " + nt.name() + " but got " + currentToken.kind() + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }
     
    private String reportSyntaxError(Token.Kind kind)
    {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected " + kind + " but got " + currentToken.kind() + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }
    
    public String errorReport()
    {
        return errorBuffer.toString();
    }
    
    public boolean hasError()
    {
        return errorBuffer.length() != 0;
    }
    
    private class QuitParseException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        public QuitParseException(String errorMessage) {
            super(errorMessage);
        }
    }
    
    private int lineNumber()
    {
        return currentToken.lineNumber();
    }
    
    private int charPosition()
    {
        return currentToken.charPosition();
    }
          
// Parser ==========================================
    private Scanner scanner;
    private Token currentToken;
    
    public Parser(Scanner scanner)
    {
        this.scanner = scanner;
        currentToken = scanner.next();
    }

    public ast.Command parse()
    {
        initSymbolTable();
        try {
            return program();
        } catch (QuitParseException q) {
            return new ast.Error(lineNumber(), charPosition(), "Could not complete parsing.");
        }
    }

    public void simpleGrammar(NonTerminal nt)
    {
        expect(nt);
    }

    // literal := INTEGER | FLOAT | TRUE | FALSE .
    public ast.Expression literal()
    {
        ast.Expression expr;
        enterRule(NonTerminal.LITERAL);

        Token tok = expectRetrieve(NonTerminal.LITERAL);
        expr = Command.newLiteral(tok);

        exitRule(NonTerminal.LITERAL);
        return expr;
    }

    // designator := IDENTIFIER { "[" expression0 "]" } .
    public void designator()
    {
        tryResolveSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expression0();
            expect(Token.Kind.CLOSE_BRACKET);
        }
    }

    // type := IDENTIFIER .
    public void type()
    {
        simpleGrammar(NonTerminal.TYPE);
    }

    // op0 := ">=" | "<=" | "!=" | "==" | ">" | "<" .
    public void op0()
    {
        simpleGrammar(NonTerminal.OP0);
    }

    // op1 := "+" | "-" | "or" .
    public void op1()
    {
        simpleGrammar(NonTerminal.OP1);
    }

    // op2 := "*" | "/" | "and" .
    public void op2()
    {
        simpleGrammar(NonTerminal.OP2);
    }

    // expression0 := expression1 [ op0 expression1 ] .
    public void expression0()
    {
        expression1();
        if(have(NonTerminal.OP0)) {
            op0();
            expression1();
        }
    }

    // expression1 := expression2 { op1  expression2 } .
    public void expression1()
    {
        expression2();
        while (have(NonTerminal.OP1)) {
            op1();
            expression2();
        }
    }

    // expression2 := expression3 { op2 expression3 } .
    public void expression2()
    {
        expression3();
        while (have(NonTerminal.OP2)) {
            op2();
            expression3();
        }
    }

    /* expression3 := "not" expression3
     * | "(" expression0 ")"
     * | designator
     * | call-expression
     * | literal .
     */
    public void expression3()
    {
        if (accept(Token.Kind.NOT)){
            expression3();
        } else if (accept(Token.Kind.OPEN_PAREN)) {
            expression0();
            expect(Token.Kind.CLOSE_PAREN);
        } else if (have(NonTerminal.DESIGNATOR)) {
            designator();
        } else if (have(NonTerminal.CALL_EXPRESSION)) {
            callExpression();
        } else if (have(NonTerminal.LITERAL)) {
            literal();
        } else {
            throw new QuitParseException(reportSyntaxError(NonTerminal.EXPRESSION3));
        }
    }

    // call-expression := "::" IDENTIFIER "(" expression-list ")" .
    public void callExpression()
    {
        expect(Token.Kind.CALL);
        tryResolveSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.OPEN_PAREN);
        expressionList();
        expect(Token.Kind.CLOSE_PAREN);
    }

    // expression-list := [ expression0 { "," expression0 } ] .
    public void expressionList()
    {
        if (have(NonTerminal.EXPRESSION0)){
            expression0();
            while (accept(Token.Kind.COMMA))
                expression0();
        }
    }

    // parameter := IDENTIFIER ":" type .
    public void parameter()
    {
        tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        type();
    }

    // parameter-list := [ parameter { "," parameter } ] .
    public void parameterList()
    {
        if (have(NonTerminal.PARAMETER)){
            parameter();
            while (accept(Token.Kind.COMMA))
                parameter();
        }
    }

    // variable-declaration := "var" IDENTIFIER ":" type ";"
    public void variableDeclaration()
    {
        expect(Token.Kind.VAR);
        tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        type();
        expect(Token.Kind.SEMICOLON);
    }

    // array-declaration := "array" IDENTIFIER ":" type "[" INTEGER "]" { "[" INTEGER "]" } ";"
    public void arrayDeclaration()
    {
        expect(Token.Kind.ARRAY);
        tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        type();
        expect(Token.Kind.OPEN_BRACKET);
        expect(Token.Kind.INTEGER);
        expect(Token.Kind.CLOSE_BRACKET);
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.INTEGER);
            expect(Token.Kind.CLOSE_BRACKET);
        }
        expect(Token.Kind.SEMICOLON);
    }

    // function-definition := "func" IDENTIFIER "(" parameter-list ")" ":" type statement-block .
    public void functionDefinition()
    {
        expect(Token.Kind.FUNC);
        tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.OPEN_PAREN);

        enterScope();

        parameterList();
        expect(Token.Kind.CLOSE_PAREN);
        expect(Token.Kind.COLON);
        type();
        statementBlock();
    }

    // declaration := variable-declaration | array-declaration | function-definition .
    public void declaration()
    {
        if (have(NonTerminal.VARIABLE_DECLARATION))
            variableDeclaration();
        else if (have(NonTerminal.ARRAY_DECLARATION))
            arrayDeclaration();
        else if (have(NonTerminal.FUNCTION_DEFINITION))
            functionDefinition();
        else
            throw new QuitParseException(reportSyntaxError(NonTerminal.DECLARATION));
    }

    // declaration-list := { declaration } .
    public void declarationList()
    {
        while (have(NonTerminal.DECLARATION))
            declaration();
    }

    // assignment-statement := "let" designator "=" expression0 ";"
    public void assignmentStatement()
    {
        expect(Token.Kind.LET);
        designator();
        expect(Token.Kind.ASSIGN);
        expression0();
        expect(Token.Kind.SEMICOLON);
    }

    // call-statement := call-expression ";"
    public void callStatement()
    {
        callExpression();
        expect(Token.Kind.SEMICOLON);
    }

    // if-statement := "if" expression0 statement-block [ "else" statement-block ] .
    public void ifStatement()
    {
        expect(Token.Kind.IF);
        expression0();

        enterScope();

        statementBlock();
        if (accept(Token.Kind.ELSE))
        {
            enterScope();

            statementBlock();
        }
    }

    // while-statement := "while" expression0 statement-block .
    public void whileStatement()
    {
        expect(Token.Kind.WHILE);
        expression0();

        enterScope();

        statementBlock();
    }

    // return-statement := "return" expression0 ";" .
    public void returnStatement()
    {
        expect(Token.Kind.RETURN);
        expression0();
        expect(Token.Kind.SEMICOLON);
    }

    /* statement := variable-declaration
     * | call-statement
     * | assignment-statement
     * | if-statement
     * | while-statement
     * | return-statement .
     */
    public void statement()
    {
        if (have(NonTerminal.VARIABLE_DECLARATION))
            variableDeclaration();
        else if (have(NonTerminal.CALL_STATEMENT))
            callStatement();
        else if (have(NonTerminal.ASSIGNMENT_STATEMENT))
            assignmentStatement();
        else if (have(NonTerminal.IF_STATEMENT))
            ifStatement();
        else if (have(NonTerminal.WHILE_STATEMENT))
            whileStatement();
        else if (have(NonTerminal.RETURN_STATEMENT))
            returnStatement();
        else
            throw new QuitParseException(reportSyntaxError(NonTerminal.STATEMENT));
    }

    // statement-list := { statement } .
    public void statementList()
    {
        while (have(NonTerminal.STATEMENT))
            statement();
    }

    // statement-block := "{" statement-list "}" .
    public void statementBlock()
    {
        expect(Token.Kind.OPEN_BRACE);
        statementList();
        expect(Token.Kind.CLOSE_BRACE);

        exitScope();
    }

    // program := declaration-list EOF .
    public ast.DeclarationList program()
    {
        throw new RuntimeException("add code to each grammar rule, to build as ast.");
    }

// Helper Methods ==========================================
    private boolean have(Token.Kind kind)
    {
        return currentToken.is(kind);
    }

    private boolean have(NonTerminal nt)
    {
        return nt.firstSet().contains(currentToken.kind());
    }

    private boolean accept(Token.Kind kind)
    {
        if (have(kind)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }    
    
    private boolean accept(NonTerminal nt)
    {
        if (have(nt)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }
   
    private boolean expect(Token.Kind kind)
    {
        if (accept(kind))
            return true;
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private boolean expect(NonTerminal nt)
    {
        if (accept(nt))
            return true;
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve(Token.Kind kind)
    {
        Token tok = currentToken;
        if (accept(kind))
            return tok;
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve(NonTerminal nt)
    {
        Token tok = currentToken;
        if (accept(nt))
            return tok;
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

// SymbolTable Management ==========================
    private SymbolTable symbolTable;
    
    private void initSymbolTable()
    {
        symbolTable = new SymbolTable(null);
    }
    
    private void enterScope()
    {
        symbolTable = new SymbolTable(symbolTable);
    }
    
    private void exitScope()
    {
        symbolTable = symbolTable.parent;
    }

    private Symbol tryResolveSymbol(Token ident)
    {
        assert(ident.is(Token.Kind.IDENTIFIER));
        String name = ident.lexeme();
        try {
            return symbolTable.lookup(name);
        } catch (SymbolNotFoundError e) {
            String message = reportResolveSymbolError(name, ident.lineNumber(), ident.charPosition());
            return new ErrorSymbol(message);
        }
    }

    private String reportResolveSymbolError(String name, int lineNum, int charPos)
    {
        String message = "ResolveSymbolError(" + lineNum + "," + charPos + ")[Could not find " + name + ".]";
        errorBuffer.append(message + "\n");
        errorBuffer.append(symbolTable.toString() + "\n");
        return message;
    }

    private Symbol tryDeclareSymbol(Token ident)
    {
        assert(ident.is(Token.Kind.IDENTIFIER));
        String name = ident.lexeme();
        try {
            return symbolTable.insert(name);
        } catch (RedeclarationError re) {
            String message = reportDeclareSymbolError(name, ident.lineNumber(), ident.charPosition());
            return new ErrorSymbol(message);
        }
    }

    private String reportDeclareSymbolError(String name, int lineNum, int charPos)
    {
        String message = "DeclareSymbolError(" + lineNum + "," + charPos + ")[" + name + " already exists.]";
        errorBuffer.append(message + "\n");
        errorBuffer.append(symbolTable.toString() + "\n");
        return message;
    }

}
