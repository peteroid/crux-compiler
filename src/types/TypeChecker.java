package types;

import java.util.Comparator;
import java.util.HashMap;
import ast.*;
import crux.Symbol;

public class TypeChecker implements CommandVisitor {
    
    private HashMap<Command, Type> typeMap;
    private StringBuffer errorBuffer;
    private Symbol currentFunction;
    private boolean returnFlag = false;
    private boolean isInIf = false;
    private int returnIfCount = 0;
    private boolean isInWhile = false;

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
        TypeList typeList = new TypeList();
        for (Expression expression : node) {
            Command command = (Command) expression;
            check(command);
            typeList.append(getType(command));
        }
        put(node, typeList);
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
        Type type = node.symbol().type();
        if (type instanceof ErrorType || type instanceof VoidType)
            put(node, new ErrorType("Variable " + node.symbol().name() +
                    " has invalid type " + type + "."));
        else
            put(node, type);
    }

    @Override
    public void visit(ArrayDeclaration node) {
        Type type = node.symbol().type();
        Type invalidBase = ((ArrayType) type).isInvalid();
        if (!(invalidBase == null))
            put(node, new ErrorType("Array " + node.symbol().name() +
                    " has invalid base type " + invalidBase + "."));
        else
            put(node, type);
    }

    @Override
    public void visit(FunctionDefinition node) {
        if (node.function().name().equals("main") &&
                !(((FuncType) node.function().type()).returnType() instanceof VoidType)) {
            put(node, new ErrorType("Function main has invalid signature."));
        }
        for (int i = 0; i < node.arguments().size(); i++) {
            Type type = node.arguments().get(i).type();
            if (type instanceof VoidType) {
                put(node, new ErrorType("Function " + node.function().name() +
                        " has a void argument in position " +
                        i + "."));
            } else if (type instanceof ErrorType) {
                put(node, new ErrorType("Function " + node.function().name() +
                        " has an error in argument in position " +
                        i + ": " + ((ErrorType) type).getMessage()));
            }
        }

        currentFunction = node.function();
        put(node, currentFunction.type());
        //explore the functions
        check(node.body());

        if (!(((FuncType) node.function().type()).returnType() instanceof VoidType) &&
                !returnFlag) {
            put(node, new ErrorType("Not all paths in function " +
                    currentFunction.name() + " have a return."));
            currentFunction = null;
        } else {
            returnFlag = false;
        }
    }

    @Override
    public void visit(Comparison node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).compare(getType(rightSide)));
    }
    
    @Override
    public void visit(Addition node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).add(getType(rightSide)));
    }
    
    @Override
    public void visit(Subtraction node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).sub(getType(rightSide)));
    }
    
    @Override
    public void visit(Multiplication node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).mul(getType(rightSide)));
    }
    
    @Override
    public void visit(Division node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).div(getType(rightSide)));
    }
    
    @Override
    public void visit(LogicalAnd node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).and(getType(rightSide)));
    }

    @Override
    public void visit(LogicalOr node) {
        Command leftSide = (Command) node.leftSide();
        Command rightSide = (Command) node.rightSide();
        check(leftSide);
        check(rightSide);
        put(node, getType(leftSide).or(getType(rightSide)));
    }

    @Override
    public void visit(LogicalNot node) {
        Command command = (Command) node.expression();
        check(command);
        put(node, getType(command).not());
    }
    
    @Override
    public void visit(Dereference node) {
        check((Command) node.expression());
        Type derefType = getType((Command) node.expression());
        put(node, derefType);
    }

    @Override
    public void visit(Index node) {
        Command base = (Command) node.base();
        Command amount = (Command) node.amount();
        check(base);
        check(amount);
        Type baseType = getType(base);

        // Fixme
        if (!(baseType instanceof ArrayType))
            baseType = baseType.deref();
        put(node, baseType.index(getType(amount)));
    }

    @Override
    public void visit(Assignment node) {
        Command destination = (Command) node.destination();
        Command source = (Command) node.source();
        check(destination);
        check(source);
        put(node, getType(destination).deref().assign(getType(source)));
//        put(node, getType(destination).assign(getType(source)));
    }

    @Override
    public void visit(Call node) {
        Command arguments = node.arguments();
        check(arguments);
        Type type = node.function().type().call(getType(arguments));
        put(node, type);
    }

    @Override
    public void visit(IfElseBranch node) {
        Command condition = (Command) node.condition();
        check(condition);

        isInIf = true;
        returnIfCount = 0;
        Type type;
        Type condType = getType(condition);
        if (getType((Command) node.condition()) instanceof BoolType) {
            Command thenBlock = (Command) node.thenBlock();
            Command elseBlock = (Command) node.elseBlock();
            check(thenBlock);
            check(elseBlock);

            type = new VoidType();
        } else {
            type = new ErrorType("IfElseBranch requires bool condition not " + condType + ".");
        }
        put(node, type);
        isInIf = false;
    }

    @Override
    public void visit(WhileLoop node) {
        Command condition = (Command) node.condition();
        check(condition);

        isInWhile = true;
        Type type;
        Type condType = getType((Command) node.condition());
        if (getType((Command) node.condition()) instanceof BoolType) {
            type = new VoidType();
        } else {
            type = new ErrorType("WhileLoop requires bool condition not " + condType + ".");
        }
        put(node, type);
        isInWhile = false;
    }

    @Override
    public void visit(Return node) {
        check((Command) node.argument());


        Type type = getType((Command) node.argument());
        Type returnType = ((FuncType) currentFunction.type()).returnType();
        if ((returnType.equivalent(type)))
            put(node, type);
        else
            put(node, new ErrorType("Function " + currentFunction.name() + " returns " + returnType + " not " + type + "."));

        returnFlag = true;

    }

    @Override
    public void visit(ast.Error node) {
        put(node, new ErrorType(node.message()));
    }
}
