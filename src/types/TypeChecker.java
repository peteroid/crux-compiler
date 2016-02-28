package types;

import java.util.HashMap;
import ast.*;
import crux.Symbol;

public class TypeChecker implements CommandVisitor {
    
    private HashMap<Command, Type> typeMap;
    private StringBuffer errorBuffer;
    private Type currentFunctionType;

    /* Useful error strings:
     *
     * "Function " + func.name() + " has a void argument in position " + pos + "."
     * "Function " + func.name() + " has an error in argument in position " + pos + ": " + error.getMessage()
     *
     * "Function main has invalid signature."
     *
     * "Not all paths in function " + currentFunctionName + " have a return."
     *
     * "IfElseBranch requires bool condition not " + condType + "."
     * "WhileLoop requires bool condition not " + condType + "."
     *
     * "Function " + currentFunctionName + " returns " + currentReturnType + " not " + retType + "."
     *
     * "Variable " + varName + " has invalid type " + varType + "."
     * "Array " + arrayName + " has invalid base type " + baseType + "."
     */

    public TypeChecker()
    {
        typeMap = new HashMap<Command, Type>();
        errorBuffer = new StringBuffer();
    }

    private void reportError(int lineNum, int charPos, String message)
    {
        errorBuffer.append("TypeError(" + lineNum + "," + charPos + ")");
        errorBuffer.append("[" + message + "]" + "\n");
    }

    private void put(Command node, Type type)
    {
        if (type instanceof ErrorType) {
            reportError(node.lineNumber(), node.charPosition(), ((ErrorType)type).getMessage());
        }
        typeMap.put(node, type);
    }

    public Type getType(Command node)
    {
        return typeMap.get(node);
    }
    
    public boolean check(Command ast)
    {
        ast.accept(this);
        return !hasError();
    }
    
    public boolean hasError()
    {
        return errorBuffer.length() != 0;
    }
    
    public String errorReport()
    {
        return errorBuffer.toString();
    }

    @Override
    public void visit(ExpressionList node) {
        put(node, new VoidType());
    }

    @Override
    public void visit(DeclarationList node) {
        for (Declaration declaration : node)
            check((Command) declaration);
    }

    @Override
    public void visit(StatementList node) {
        put(node, new VoidType());

        for (Statement statement : node)
            check((Command) statement);
    }

    // FIXME
    @Override
    public void visit(AddressOf node) {
        put(node, node.symbol().type());
    }

    @Override
    public void visit(LiteralBool node) {
        put(node, new BoolType());
    }

    @Override
    public void visit(LiteralFloat node) {
        put(node, new FloatType());
    }

    @Override
    public void visit(LiteralInt node) {
        put(node, new IntType());
    }

    @Override
    public void visit(VariableDeclaration node) {
        put(node, node.symbol().type());
    }

    @Override
    public void visit(ArrayDeclaration node) {
        put(node, node.symbol().type());
    }

    @Override
    public void visit(FunctionDefinition node) {
        if (node.function().name().equals("main") &&
                !(((FuncType) node.function().type()).returnType() instanceof VoidType)) {
            errorBuffer.append("Function main has invalid signature.");
        }
        for (Symbol symbol : node.arguments()) {
            if (symbol.type() instanceof VoidType) {
                errorBuffer.append("Function " + node.function().name() +
                        " has a void argument in position " +
                        node.arguments().indexOf(symbol) + ".");
            }
        }

        currentFunctionType = node.function().type();
        if (hasError()) {
            put(node, new ErrorType(errorReport()));
            errorBuffer = new StringBuffer();
        } else {
            put(node, currentFunctionType);
        }

        //explore the functions
        check(node.body());
    }

    @Override
    public void visit(Comparison node) {
        put(node, getType((Command) node.leftSide()).compare(getType((Command) node.rightSide())));
    }
    
    @Override
    public void visit(Addition node) {
        put(node, getType((Command) node.leftSide()).add(getType((Command) node.rightSide())));
    }
    
    @Override
    public void visit(Subtraction node) {
        put(node, getType((Command) node.leftSide()).sub(getType((Command) node.rightSide())));
    }
    
    @Override
    public void visit(Multiplication node) {
        put(node, getType((Command) node.leftSide()).mul(getType((Command) node.rightSide())));
    }
    
    @Override
    public void visit(Division node) {
        put(node, getType((Command) node.leftSide()).div(getType((Command) node.rightSide())));
    }
    
    @Override
    public void visit(LogicalAnd node) {
        put(node, getType((Command) node.leftSide()).and(getType((Command) node.rightSide())));
    }

    @Override
    public void visit(LogicalOr node) {
        put(node, getType((Command) node.leftSide()).or(getType((Command) node.rightSide())));
    }

    @Override
    public void visit(LogicalNot node) {
        put(node, getType((Command) node.expression()).not());
    }
    
    @Override
    public void visit(Dereference node) {
        put(node, getType((Command) node.expression()).deref());
    }

    @Override
    public void visit(Index node) {
        put(node, getType((Command) node.base()).index(getType((Command) node.amount())));
    }

    @Override
    public void visit(Assignment node) {
        put(node, getType((Command) node.destination()).assign(getType((Command) node.source())));
    }

    @Override
    public void visit(Call node) {
        put(node, node.function().type().call(getType(node.arguments())));
    }

    @Override
    public void visit(IfElseBranch node) {
        Type type;
        Type condType = getType((Command) node.condition());
        if (getType((Command) node.condition()) instanceof BoolType) {
            type = new VoidType();
        } else {
            type = new ErrorType("IfElseBranch requires bool condition not " + condType + ".");
        }
        put(node, type);
    }

    @Override
    public void visit(WhileLoop node) {
        Type type;
        Type condType = getType((Command) node.condition());
        if (getType((Command) node.condition()) instanceof BoolType) {
            type = new VoidType();
        } else {
            type = new ErrorType("WhileLoop requires bool condition not " + condType + ".");
        }
        put(node, type);
    }

    @Override
    public void visit(Return node) {
        put(node, getType((Command) node.argument()));
    }

    @Override
    public void visit(ast.Error node) {
        put(node, new ErrorType(node.message()));
    }
}
