package crux;

import ast.*;
import types.*;

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
        parseTreeBuffer.append(lineData + "\n");
        parseTreeRecursionDepth++;
    }

    private void exitRule(NonTerminal nonTerminal)
    {
        parseTreeRecursionDepth--;
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

    public Token simpleGrammar(NonTerminal nt)
    {
        enterRule(nt);
        Token token = expectRetrieve(nt);
        exitRule(nt);
        return token;
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
    public Expression designator(boolean isAssignment)
    {
        enterRule(NonTerminal.DESIGNATOR);

        Token token = new Token(this.currentToken);
        Expression expression = new AddressOf(
                lineNumber(),
                charPosition(),
                tryResolveSymbol(expectRetrieve(Token.Kind.IDENTIFIER)));
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expression = new Index(
                    lineNumber(),
                    charPosition(),
                    expression,
                    expression0());
            expect(Token.Kind.CLOSE_BRACKET);
        }

        exitRule(NonTerminal.DESIGNATOR);
        if (!isAssignment)
            return new Dereference(token.lineNumber(), token.charPosition(), expression);
        else
            return expression;
    }

    // default methods
    public Expression designator()
    {
        return designator(false);
    }

    // type := IDENTIFIER .
    public Type type()
    {
        Token token = simpleGrammar(NonTerminal.TYPE);
        return Type.getBaseType(token.lexeme());
    }

    // op0 := ">=" | "<=" | "!=" | "==" | ">" | "<" .
    public Token op0()
    {
        return simpleGrammar(NonTerminal.OP0);
    }

    // op1 := "+" | "-" | "or" .
    public Token op1()
    {
        return simpleGrammar(NonTerminal.OP1);
    }

    // op2 := "*" | "/" | "and" .
    public Token op2()
    {
        return simpleGrammar(NonTerminal.OP2);
    }

    // expression0 := expression1 [ op0 expression1 ] .
    public Expression expression0()
    {
        enterRule(NonTerminal.EXPRESSION0);

        Expression expression = expression1();
        if(have(NonTerminal.OP0)) {
            expression = Command.newExpression(expression, op0(), expression1());
        }

        exitRule(NonTerminal.EXPRESSION0);
        return expression;
    }

    // expression1 := expression2 { op1  expression2 } .
    public Expression expression1()
    {
        enterRule(NonTerminal.EXPRESSION1);

        Expression expression = expression2();
        while (have(NonTerminal.OP1)) {
            expression = Command.newExpression(expression, op1(), expression2());
        }

        exitRule(NonTerminal.EXPRESSION1);
        return expression;
    }

    // expression2 := expression3 { op2 expression3 } .
    public Expression expression2()
    {
        enterRule(NonTerminal.EXPRESSION2);

        Expression expression = expression3();
        while (have(NonTerminal.OP2)) {
            expression = Command.newExpression(expression, op2(), expression3());
        }

        exitRule(NonTerminal.EXPRESSION2);
        return expression;
    }

    /* expression3 := "not" expression3
     * | "(" expression0 ")"
     * | designator
     * | call-expression
     * | literal .
     */
    public Expression expression3()
    {
        enterRule(NonTerminal.EXPRESSION3);

        Expression expression;
        Token token = new Token(this.currentToken);
        if (accept(Token.Kind.NOT)){
            expression = Command.newExpression(expression3(), token, null);
        } else if (accept(Token.Kind.OPEN_PAREN)) {
            expression = expression0();
            expect(Token.Kind.CLOSE_PAREN);
        } else if (have(NonTerminal.DESIGNATOR)) {
            expression = designator();
        } else if (have(NonTerminal.CALL_EXPRESSION)) {
            expression = callExpression();
        } else if (have(NonTerminal.LITERAL)) {
            expression = literal();
        } else {
            throw new QuitParseException(reportSyntaxError(NonTerminal.EXPRESSION3));
        }

        exitRule(NonTerminal.EXPRESSION3);
        return expression;
    }

    // call-expression := "::" IDENTIFIER "(" expression-list ")" .
    public Expression callExpression()
    {
        enterRule(NonTerminal.CALL_EXPRESSION);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.CALL);
        Symbol symbol = tryResolveSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.OPEN_PAREN);
        ExpressionList expressionList = expressionList();
        expect(Token.Kind.CLOSE_PAREN);

        exitRule(NonTerminal.CALL_EXPRESSION);
        return new Call(token.lineNumber(), token.charPosition(), symbol, expressionList);
    }

    // expression-list := [ expression0 { "," expression0 } ] .
    public ExpressionList expressionList()
    {
        enterRule(NonTerminal.EXPRESSION_LIST);

        ExpressionList expressionList = new ExpressionList(
                lineNumber(),
                charPosition());
        if (have(NonTerminal.EXPRESSION0)){
            expressionList.add(expression0());
            while (accept(Token.Kind.COMMA))
                expressionList.add(expression0());
        }

        exitRule(NonTerminal.EXPRESSION_LIST);
        return expressionList;
    }

    // parameter := IDENTIFIER ":" type .
    public Declaration parameter()
    {
        enterRule(NonTerminal.VARIABLE_DECLARATION);

        Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        VariableDeclaration variableDeclaration = new VariableDeclaration(
                lineNumber(),
                charPosition(),
                symbol
        );
        expect(Token.Kind.COLON);
        Type type = type();
        symbol.setType(type);

        exitRule(NonTerminal.VARIABLE_DECLARATION);
        return variableDeclaration;
    }

    // parameter-list := [ parameter { "," parameter } ] .
    public DeclarationList parameterList()
    {
        enterRule(NonTerminal.PARAMETER_LIST);

        DeclarationList declarationList = new DeclarationList(
                lineNumber(),
                charPosition());
        if (have(NonTerminal.PARAMETER)){
            declarationList.add(parameter());
            while (accept(Token.Kind.COMMA))
                declarationList.add(parameter());
        }

        exitRule(NonTerminal.PARAMETER_LIST);
        return declarationList;
    }

    // variable-declaration := "var" IDENTIFIER ":" type ";"
    public Declaration variableDeclaration()
    {
        enterRule(NonTerminal.VARIABLE_DECLARATION);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.VAR);
        Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        Type type = type();
        symbol.setType(type);
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.VARIABLE_DECLARATION);
        return new VariableDeclaration(
                token.lineNumber(),
                token.charPosition(),
                symbol);
    }

    // array-declaration := "array" IDENTIFIER ":" type "[" INTEGER "]" { "[" INTEGER "]" } ";"
    public Declaration arrayDeclaration()
    {
        enterRule(NonTerminal.ARRAY_DECLARATION);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.ARRAY);
        Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.COLON);
        Type type = type();
        expect(Token.Kind.OPEN_BRACKET);
        Token intToken = expectRetrieve(Token.Kind.INTEGER);
        // FIXME
        symbol.setType(new ArrayType(Integer.parseInt(intToken.lexeme()), type));
        expect(Token.Kind.CLOSE_BRACKET);
        while (accept(Token.Kind.OPEN_BRACKET)) {
            expect(Token.Kind.INTEGER);
            expect(Token.Kind.CLOSE_BRACKET);
        }
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.ARRAY_DECLARATION);
        return new ArrayDeclaration(
                token.lineNumber(),
                token.charPosition(),
                symbol
        );
    }

    // fixme: toSymbolList() is uncertain
    // function-definition := "func" IDENTIFIER "(" parameter-list ")" ":" type statement-block .
    public Declaration functionDefinition()
    {
        enterRule(NonTerminal.FUNCTION_DEFINITION);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.FUNC);
        Symbol symbol = tryDeclareSymbol(expectRetrieve(Token.Kind.IDENTIFIER));
        expect(Token.Kind.OPEN_PAREN);

        enterScope();

        DeclarationList parameterList = parameterList();
        expect(Token.Kind.CLOSE_PAREN);
        expect(Token.Kind.COLON);
        Type type = type();
        symbol.setType(new FuncType(parameterList.toTypeList(), type));
        StatementList statementList = statementBlock();

        exitRule(NonTerminal.FUNCTION_DEFINITION);
        return new FunctionDefinition(
                token.lineNumber(),
                token.charPosition(),
                symbol,
                parameterList.toSymbolList(),
                statementList
        );
    }

    // declaration := variable-declaration | array-declaration | function-definition .
    public Declaration declaration()
    {
        enterRule(NonTerminal.DECLARATION);

        Declaration declaration;
        if (have(NonTerminal.VARIABLE_DECLARATION))
            declaration = variableDeclaration();
        else if (have(NonTerminal.ARRAY_DECLARATION))
            declaration = arrayDeclaration();
        else if (have(NonTerminal.FUNCTION_DEFINITION))
            declaration = functionDefinition();
        else
            throw new QuitParseException(reportSyntaxError(NonTerminal.DECLARATION));

        exitRule(NonTerminal.DECLARATION);
        return declaration;
    }

    // declaration-list := { declaration } .
    public DeclarationList declarationList()
    {
        enterRule(NonTerminal.DECLARATION_LIST);

        DeclarationList declarationList = new DeclarationList(
                lineNumber(),
                charPosition()
        );
        while (have(NonTerminal.DECLARATION))
            declarationList.add(declaration());

        exitRule(NonTerminal.DECLARATION_LIST);
        return declarationList;
    }

    // assignment-statement := "let" designator "=" expression0 ";"
    public Statement assignmentStatement()
    {
        enterRule(NonTerminal.ASSIGNMENT_STATEMENT);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.LET);
        Expression dest = designator(true);
        expect(Token.Kind.ASSIGN);
        Expression source = expression0();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.ASSIGNMENT_STATEMENT);
        return new Assignment(
                token.lineNumber(),
                token.charPosition(),
                dest,
                source
        );
    }

    // call-statement := call-expression ";"
    public Statement callStatement()
    {
        enterRule(NonTerminal.CALL_STATEMENT);

        Statement statement = (Call) callExpression();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.CALL_STATEMENT);
        return statement;
    }

    // if-statement := "if" expression0 statement-block [ "else" statement-block ] .
    public Statement ifStatement()
    {
        enterRule(NonTerminal.IF_STATEMENT);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.IF);
        Expression condition = expression0();

        enterScope();

        StatementList thenBlock = statementBlock();
        StatementList elseBlock = new StatementList(
                lineNumber(),
                charPosition()
        );
        if (accept(Token.Kind.ELSE))
        {
            enterScope();

            elseBlock = statementBlock();
        }

        exitRule(NonTerminal.IF_STATEMENT);
        return new IfElseBranch(
                token.lineNumber(),
                token.charPosition(),
                condition,
                thenBlock,
                elseBlock
        );
    }

    // while-statement := "while" expression0 statement-block .
    public Statement whileStatement()
    {
        enterRule(NonTerminal.WHILE_STATEMENT);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.WHILE);
        Expression condition = expression0();

        enterScope();

        StatementList block = statementBlock();

        exitRule(NonTerminal.WHILE_STATEMENT);
        return new WhileLoop(
                token.lineNumber(),
                token.charPosition(),
                condition,
                block
        );
    }

    // return-statement := "return" expression0 ";" .
    public Statement returnStatement()
    {
        enterRule(NonTerminal.RETURN_STATEMENT);

        Token token = new Token(this.currentToken);
        expect(Token.Kind.RETURN);
        Expression expression = expression0();
        expect(Token.Kind.SEMICOLON);

        exitRule(NonTerminal.RETURN_STATEMENT);
        return new Return(
                token.lineNumber(),
                token.charPosition(),
                expression
        );
    }

    /* statement := variable-declaration
     * | call-statement
     * | assignment-statement
     * | if-statement
     * | while-statement
     * | return-statement .
     */
    public Statement statement()
    {
        enterRule(NonTerminal.STATEMENT);

        Statement statement;
        if (have(NonTerminal.VARIABLE_DECLARATION))
            statement = (Statement) variableDeclaration();
        else if (have(NonTerminal.CALL_STATEMENT))
            statement = callStatement();
        else if (have(NonTerminal.ASSIGNMENT_STATEMENT))
            statement = assignmentStatement();
        else if (have(NonTerminal.IF_STATEMENT))
            statement = ifStatement();
        else if (have(NonTerminal.WHILE_STATEMENT))
            statement = whileStatement();
        else if (have(NonTerminal.RETURN_STATEMENT))
            statement = returnStatement();
        else
            throw new QuitParseException(reportSyntaxError(NonTerminal.STATEMENT));

        exitRule(NonTerminal.STATEMENT);
        return statement;
    }

    // statement-list := { statement } .
    public StatementList statementList()
    {
        enterRule(NonTerminal.STATEMENT_LIST);

        StatementList statementList = new StatementList(
                lineNumber(),
                charPosition()
        );
        while (have(NonTerminal.STATEMENT))
            statementList.add(statement());

        exitRule(NonTerminal.STATEMENT_LIST);
        return statementList;
    }

    // statement-block := "{" statement-list "}" .
    public StatementList statementBlock()
    {
        enterRule(NonTerminal.STATEMENT_BLOCK);

        expect(Token.Kind.OPEN_BRACE);
        StatementList statementList = statementList();
        expect(Token.Kind.CLOSE_BRACE);

        exitScope();
        exitRule(NonTerminal.STATEMENT_BLOCK);
        return statementList;
    }

    // program := declaration-list EOF .
    public ast.DeclarationList program()
    {
        enterRule(NonTerminal.PROGRAM);

        DeclarationList declarationList = declarationList();

        exitRule(NonTerminal.PROGRAM);
        return declarationList;
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
// Typing System ===================================

    private Type tryResolveType(String typeStr)
    {
        return Type.getBaseType(typeStr);
    }
}
