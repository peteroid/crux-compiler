package crux;

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
    
    public void parse()
    {
        try {
            program();
        } catch (QuitParseException q) {
            errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            errorBuffer.append("[Could not complete parsing.]");
        }
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
        //return false;
    }

    private boolean expect(NonTerminal nt)
    {
        if (accept(nt))
            return true;
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
        //return false;
    }
   
// Grammar Rules =====================================================

    public void simpleGrammar(NonTerminal nt)
    {
        enterRule(nt);
        expect(nt);
        exitRule(nt);
    }
    
    // literal := INTEGER | FLOAT | TRUE | FALSE .
    public void literal()
    {
        simpleGrammar(NonTerminal.LITERAL);
    }
    
    // designator := IDENTIFIER { "[" expression0 "]" } .
    public void designator()
    {
        enterRule(NonTerminal.DESIGNATOR);

        expect(Token.Kind.IDENTIFIER);
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expression0();
            expect(Token.Kind.CLOSE_BRACKET);
        }
        
        exitRule(NonTerminal.DESIGNATOR);
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
        enterRule(NonTerminal.EXPRESSION0);

        expression1();
        if(have(NonTerminal.OP0)) {
            op0();
            expression1();
        }


        exitRule(NonTerminal.EXPRESSION0);
    }

    // expression1 := expression2 { op1  expression2 } .
    public void expression1()
    {
        enterRule(NonTerminal.EXPRESSION1);

        expression2();
        while (have(NonTerminal.OP1)) {
            op1();
            expression2();
        }


        exitRule(NonTerminal.EXPRESSION1);
    }

    // expression2 := expression3 { op2 expression3 } .
    public void expression2()
    {
        enterRule(NonTerminal.EXPRESSION2);

        expression3();
        while (have(NonTerminal.OP2)) {
            op2();
            expression3();
        }

        exitRule(NonTerminal.EXPRESSION2);
    }

    /* expression3 := "not" expression3
     * | "(" expression0 ")"
     * | designator
     * | call-expression
     * | literal .
     */
    public void expression3()
    {
        enterRule(NonTerminal.EXPRESSION3);

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

        exitRule(NonTerminal.EXPRESSION3);
    }

    // call-expression := "::" IDENTIFIER "(" expression-list ")" .
    public void callExpression()
    {
        enterRule(NonTerminal.CALL_EXPRESSION);

        expect(Token.Kind.CALL);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.OPEN_PAREN);
        expressionList();
        expect(Token.Kind.CLOSE_PAREN);

        exitRule(NonTerminal.CALL_EXPRESSION);
    }

    // expression-list := [ expression0 { "," expression0 } ] .
    public void expressionList()
    {
        enterRule(NonTerminal.EXPRESSION_LIST);

        if (have(NonTerminal.EXPRESSION0)){
            expression0();
            while (accept(Token.Kind.COMMA))
                expression0();
        }

        exitRule(NonTerminal.EXPRESSION_LIST);
    }

    // parameter := IDENTIFIER ":" type .
    public void parameter()
    {
        enterRule(NonTerminal.PARAMETER);

        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.COLON);
        type();

        exitRule(NonTerminal.PARAMETER);
    }

    // parameter-list := [ parameter { "," parameter } ] .
    public void parameterList()
    {
        enterRule(NonTerminal.PARAMETER_LIST);

        if (have(NonTerminal.PARAMETER)){
            parameter();
            while (accept(Token.Kind.COMMA))
                parameter();
        }

        exitRule(NonTerminal.PARAMETER_LIST);
    }

    // variable-declaration := "var" IDENTIFIER ":" type ";"
    public void variableDeclaration()
    {
        enterRule(NonTerminal.VARIABLE_DECLARATION);

        expect(Token.Kind.VAR);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.COLON);
        type();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.VARIABLE_DECLARATION);
    }

    // array-declaration := "array" IDENTIFIER ":" type "[" INTEGER "]" { "[" INTEGER "]" } ";"
    public void arrayDeclaration()
    {
        enterRule(NonTerminal.ARRAY_DECLARATION);

        expect(Token.Kind.ARRAY);
        expect(Token.Kind.IDENTIFIER);
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

        exitRule(NonTerminal.ARRAY_DECLARATION);
    }

    // function-definition := "func" IDENTIFIER "(" parameter-list ")" ":" type statement-block .
    public void functionDefinition()
    {
        enterRule(NonTerminal.FUNCTION_DEFINITION);

        expect(Token.Kind.FUNC);
        expect(Token.Kind.IDENTIFIER);
        expect(Token.Kind.OPEN_PAREN);
        parameterList();
        expect(Token.Kind.CLOSE_PAREN);
        expect(Token.Kind.COLON);
        type();
        statementBlock();

        exitRule(NonTerminal.FUNCTION_DEFINITION);
    }

    // declaration := variable-declaration | array-declaration | function-definition .
    public void declaration()
    {
        enterRule(NonTerminal.DECLARATION);

        if (have(NonTerminal.VARIABLE_DECLARATION))
            variableDeclaration();
        else if (have(NonTerminal.ARRAY_DECLARATION))
            arrayDeclaration();
        else if (have(NonTerminal.FUNCTION_DEFINITION))
            functionDefinition();
        else
            throw new QuitParseException(reportSyntaxError(NonTerminal.DECLARATION));

        exitRule(NonTerminal.DECLARATION);
    }

    // declaration-list := { declaration } .
    public void declarationList()
    {
        enterRule(NonTerminal.DECLARATION_LIST);

        while (have(NonTerminal.DECLARATION))
            declaration();

        exitRule(NonTerminal.DECLARATION_LIST);
    }

    // assignment-statement := "let" designator "=" expression0 ";"
    public void assignmentStatement()
    {
        enterRule(NonTerminal.ASSIGNMENT_STATEMENT);

        expect(Token.Kind.LET);
        designator();
        expect(Token.Kind.ASSIGN);
        expression0();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.ASSIGNMENT_STATEMENT);
    }

    // call-statement := call-expression ";"
    public void callStatement()
    {
        enterRule(NonTerminal.CALL_STATEMENT);

        callExpression();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.CALL_STATEMENT);
    }

    // if-statement := "if" expression0 statement-block [ "else" statement-block ] .
    public void ifStatement()
    {
        enterRule(NonTerminal.IF_STATEMENT);

        expect(Token.Kind.IF);
        expression0();
        statementBlock();
        if (accept(Token.Kind.ELSE))
            statementBlock();

        exitRule(NonTerminal.IF_STATEMENT);
    }

    // while-statement := "while" expression0 statement-block .
    public void whileStatement()
    {
        enterRule(NonTerminal.WHILE_STATEMENT);

        expect(Token.Kind.WHILE);
        expression0();
        statementBlock();

        exitRule(NonTerminal.WHILE_STATEMENT);
    }

    // return-statement := "return" expression0 ";" .
    public void returnStatement()
    {
        enterRule(NonTerminal.RETURN_STATEMENT);

        expect(Token.Kind.RETURN);
        expression0();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.RETURN_STATEMENT);
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
        enterRule(NonTerminal.STATEMENT);

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

        exitRule(NonTerminal.STATEMENT);
    }

    // statement-list := { statement } .
    public void statementList()
    {
        enterRule(NonTerminal.STATEMENT_LIST);

        while (have(NonTerminal.STATEMENT))
            statement();

        exitRule(NonTerminal.STATEMENT_LIST);
    }

    // statement-block := "{" statement-list "}" .
    public void statementBlock()
    {
        enterRule(NonTerminal.STATEMENT_BLOCK);

        expect(Token.Kind.OPEN_BRACE);
        statementList();
        expect(Token.Kind.CLOSE_BRACE);

        exitRule(NonTerminal.STATEMENT_BLOCK);
    }

    // program := declaration-list EOF .
    public void program()
    {
        enterRule(NonTerminal.PROGRAM);

        declarationList();
        expect(Token.Kind.EOF);

        exitRule(NonTerminal.PROGRAM);
    }
    
}
