package mips;

import java.util.regex.Pattern;

import ast.*;
import crux.SymbolTable;
import types.*;

public class CodeGen implements ast.CommandVisitor {
    
    private StringBuffer errorBuffer = new StringBuffer();
    private TypeChecker tc;
    private Program program;
    private ActivationRecord currentFunction;
    private int sp = 0;
    private int fp = 0;

    public CodeGen(TypeChecker tc)
    {
        this.tc = tc;
        this.program = new Program();
    }
    
    public boolean hasError()
    {
        return errorBuffer.length() != 0;
    }
    
    public String errorReport()
    {
        return errorBuffer.toString();
    }

    private class CodeGenException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        public CodeGenException(String errorMessage) {
            super(errorMessage);
        }
    }
    
    public boolean generate(Command ast)
    {
        try {
            currentFunction = ActivationRecord.newGlobalFrame();
            ast.accept(this);
            return !hasError();
        } catch (CodeGenException e) {
            return false;
        }
    }
    
    public Program getProgram()
    {
        return program;
    }

    @Override
    public void visit(ExpressionList node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(DeclarationList node) {
        for (Declaration declaration : node) {
            declaration.accept(this);
        }
    }

    @Override
    public void visit(StatementList node) {
        for (Statement statement : node) {
            statement.accept(this);
        }
    }

    @Override
    public void visit(AddressOf node) {
        currentFunction.getAddress(getProgram(), "$t0", node.symbol());
    }

    @Override
    public void visit(LiteralBool node) {
        String boolString = node.value() == LiteralBool.Value.TRUE? "1" : "0";
        sp = getProgram().appendInstruction("li $t0, " + boolString);
        sp = getProgram().appendInstruction("sw $t0, 0($sp)");
    }

    @Override
    public void visit(LiteralFloat node) {
        sp = getProgram().appendInstruction("li.s $f0, " + node.value().toString());
        getProgram().pushFloat("$f0");
    }

    @Override
    public void visit(LiteralInt node) {
        sp = getProgram().appendInstruction("li $t0, " + node.value().toString());
        getProgram().pushInt("$t0");
    }

    @Override
    public void visit(VariableDeclaration node) {
        currentFunction.add(getProgram(), node);
    }

    @Override
    public void visit(ArrayDeclaration node) {
        currentFunction.add(getProgram(), node);
    }

    @Override
    public void visit(FunctionDefinition node) {
        String functionLabel = "func." + node.function().name() + ":";
        if (node.function().name().equals("main"))
            functionLabel = "" + node.function().name() + ":";
        sp = getProgram().appendInstruction(functionLabel);
        getProgram().insertPrologue(sp + 1, node.arguments().size() * 4);
        generate(node.body());
        // local vars reservation

        getProgram().appendEpilogue(node.arguments().size() * 4);
    }

    @Override
    public void visit(Addition node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);
        if (tc.getType(node) instanceof IntType) {

        } else if (tc.getType(node) instanceof FloatType) {

        }
    }

    @Override
    public void visit(Subtraction node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(Multiplication node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(Division node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(LogicalAnd node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(LogicalOr node) {
        throw new RuntimeException("Implement this");
    }
    
    @Override
    public void visit(LogicalNot node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(Comparison node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(Dereference node) {
        node.expression().accept(this);
        getProgram().appendInstruction("lw $t1, 0($t0)");
        getProgram().appendInstruction("sw $t1, 0($sp)");
    }

    @Override
    public void visit(Index node) {
        // $t0[$sp]
        node.amount().accept(this);
        getProgram().appendInstruction("lw $t2, 0($sp)");
        getProgram().appendInstruction("li $t3, 4");
        getProgram().appendInstruction("mul $t2, $t2, $t3");

        node.base().accept(this);
        getProgram().appendInstruction("add $t0, $t0, $t2");
    }

    @Override
    public void visit(Assignment node) {
        node.source().accept(this); // data on stack
        getProgram().appendInstruction("lw $t1, 0($sp)");

        node.destination().accept(this); // address on $t0
        getProgram().appendInstruction("sw $t1, 0($t0)");
    }

    @Override
    public void visit(Call node) {
//        getProgram().insertPrologue(sp, node.arguments().size() * 4);
        if (SymbolTable.isPredefined(node.function().name())) {
            for (Expression expression : node.arguments()) {
                expression.accept(this);
            }
        } else {
            for (Expression expression : node.arguments()) {
                expression.accept(this);
            }
        }
        sp = getProgram().appendInstruction("jal func." + node.function().name());
    }

    @Override
    public void visit(IfElseBranch node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(WhileLoop node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(Return node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(ast.Error node) {
        String message = "CodeGen cannot compile a " + node;
        errorBuffer.append(message);
        throw new CodeGenException(message);
    }
}
