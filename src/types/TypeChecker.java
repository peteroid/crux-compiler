package types;

import java.util.Comparator;
import java.util.HashMap;
import ast.*;
import crux.Symbol;

public class TypeChecker implements CommandVisitor {
    
    private HashMap<Command, Type> typeMap;
    private StringBuffer errorBuffer;
    private Symbol currentFunction;

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
        Type returnType = new VoidType();

        for (Statement statement : node) {
            Command command = (Command) statement;
            check(command);
            Type type = getType(command);

            if (!(type instanceof VoidType)) {
                returnType = getType(command);
            }
        }

        typeMap.put(node, returnType);
    }

    @Override
    public void visit(AddressOf node) {
        put(node, node.symbol().type().deref());
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
            put(node, new VoidType());
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
        currentFunction = node.function();
        Type expectedReturnType = ((FuncType) currentFunction.type()).returnType();

        if (node.function().name().equals("main") &&
                !(expectedReturnType instanceof VoidType)) {
            put(node, new ErrorType("Function main has invalid signature."));
        } else {
            for (int i = 0; i < node.arguments().size(); i++) {
                Type type = node.arguments().get(i).type();
                if (type instanceof VoidType) {
                    put(node, new ErrorType("Function " + currentFunction.name() +
                            " has a void argument in position " +
                            i + "."));
                    return;
                } else if (type instanceof ErrorType) {
                    put(node, new ErrorType("Function " + currentFunction.name() +
                            " has an error in argument in position " +
                            i + ": " + ((ErrorType) type).getMessage()));
                    return;
                }
            }

            //explore the functions
            check(node.body());

            Type actualReturnType = getType(node.body());
            if (!(expectedReturnType instanceof VoidType) && actualReturnType instanceof VoidType) {
                put(node, new ErrorType("Not all paths in function " +
                        currentFunction.name() + " have a return."));
            }

            typeMap.put(node, actualReturnType);
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
        put(node, derefType.deref());
    }

    @Override
    public void visit(Index node) {
        Command base = (Command) node.base();
        Command amount = (Command) node.amount();
        check(base);
        check(amount);
        Type baseType = getType(base);
        Type amountType = getType(amount);
        put(node, baseType.index(amountType));
    }

    @Override
    public void visit(Assignment node) {
        Command destination = (Command) node.destination();
        Command source = (Command) node.source();
        check(destination);
        check(source);
        Type destType = getType(destination);
        put(node, destType.assign(getType(source)));
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

        Type type;
        Type condType = getType(condition);
        if (getType((Command) node.condition()) instanceof BoolType) {
            Command thenBlock = node.thenBlock();
            Command elseBlock = node.elseBlock();
            check(thenBlock);
            check(elseBlock);

            if (getType(thenBlock) instanceof VoidType || getType(elseBlock) instanceof VoidType)
                type = new VoidType();
            else
                type = getType(thenBlock); // either one should be fine
        } else {
            type = new ErrorType("IfElseBranch requires bool condition not " + condType + ".");
        }
        put(node, type);
    }

    @Override
    public void visit(WhileLoop node) {
        Command condition = (Command) node.condition();
        check(condition);

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
        Command argument = (Command) node.argument();
        check(argument);
        Type actualReturnType = getType(argument);
        Type expectedReturnType = ((FuncType) currentFunction.type()).returnType();

        if (!actualReturnType.equivalent(expectedReturnType)) {
            put(node, new ErrorType("Function " + currentFunction.name() +
                    " returns " + expectedReturnType  + " not " +
                    actualReturnType+ "."));
        } else {
            // can be either one returnType
            put(node, actualReturnType);
        }
    }

    @Override
    public void visit(ast.Error node) {
        put(node, new ErrorType(node.message()));
    }
}
