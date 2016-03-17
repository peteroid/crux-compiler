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

            // pop the stack if there is a non-void call
            if (statement instanceof Call) {
                FuncType callFuncType = (FuncType) ((Call) statement).function().type();
                if (!(callFuncType.returnType() instanceof VoidType)) {
                    // shift the stack
                    getProgram().appendInstruction("addi $sp, $sp, 4");
                }
            }
        }
    }

    @Override
    public void visit(AddressOf node) {
        currentFunction.getAddress(getProgram(), "$t0", node.symbol());
    }

    @Override
    public void visit(LiteralBool node) {
        String boolString = node.value() == LiteralBool.Value.TRUE? "1" : "0";
        getProgram().appendInstruction("addi $sp, $sp, -4");
        getProgram().appendInstruction("li $t0, " + boolString);
        getProgram().appendInstruction("sw $t0, 0($sp)");
    }

    @Override
    public void visit(LiteralFloat node) {
        getProgram().appendInstruction("li.s $f0, " + node.value().toString());
        getProgram().pushFloat("$f0");
    }

    @Override
    public void visit(LiteralInt node) {
        getProgram().appendInstruction("li $t0, " + node.value().toString());
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
        int labelPosition = getProgram().appendInstruction(functionLabel);

        // set the new scope
        currentFunction = new ActivationRecord(node, currentFunction);
        node.body().accept(this);

        // insert before the body with the size of local vars
        int localVarsSize = currentFunction.stackSize();
        getProgram().insertPrologue(labelPosition + 1, localVarsSize);

        getProgram().appendEpilogue(localVarsSize);

        // restore the scope
        currentFunction = currentFunction.parent();
    }

    @Override
    public void visit(Addition node) {
        if (tc.getType(node) instanceof IntType) {
            node.leftSide().accept(this);
            getProgram().popInt("$t1");
            node.rightSide().accept(this);
            getProgram().popInt("$t2");
            getProgram().appendInstruction("add $t1, $t1, $t2");
            getProgram().pushInt("$t1");
        } else if (tc.getType(node) instanceof FloatType) {
            node.leftSide().accept(this);
            getProgram().popFloat("$f1");
            node.rightSide().accept(this);
            getProgram().popFloat("$f2");
            getProgram().appendInstruction("add.s $f1, $f1, $f2");
            getProgram().pushFloat("$f1");
        }
    }

    @Override
    public void visit(Subtraction node) {
        if (tc.getType(node) instanceof IntType) {
            node.leftSide().accept(this);
            getProgram().popInt("$t1");
            node.rightSide().accept(this);
            getProgram().popInt("$t2");
            getProgram().appendInstruction("sub $t1, $t1, $t2");
            getProgram().pushInt("$t1");
        } else if (tc.getType(node) instanceof FloatType) {
            node.leftSide().accept(this);
            getProgram().popFloat("$f1");
            node.rightSide().accept(this);
            getProgram().popFloat("$f2");
            getProgram().appendInstruction("sub.s $f1, $f1, $f2");
            getProgram().pushFloat("$f1");
        }
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
        //getProgram().appendInstruction("sw $t1, 0($sp)");
        getProgram().pushInt("$t1"); // fixme: can guarantee as an int?
    }

    @Override
    public void visit(Index node) {
        // $t0[$sp]
        node.amount().accept(this);
        getProgram().popInt("$t2");
        getProgram().appendInstruction("li $t3, 4");
        getProgram().appendInstruction("mul $t2, $t2, $t3");

        node.base().accept(this);
        getProgram().appendInstruction("add $t0, $t0, $t2");
    }

    @Override
    public void visit(Assignment node) {
        node.source().accept(this); // data on stack
        Type sourceType = tc.getType((Command) node.source());
        if (sourceType instanceof IntType) {
            getProgram().popInt("$t1");
        } else if (sourceType instanceof FloatType) {
            getProgram().popFloat("$t1");
        } else {
            throw new CodeGenException("Unknown type in assignment: " + sourceType.toString());
        }

        node.destination().accept(this); // address on $t0
        getProgram().appendInstruction("sw $t1, 0($t0)");
    }

    @Override
    public void visit(Call node) {
        for (Expression expression : node.arguments()) {
            expression.accept(this);
        }

        getProgram().appendInstruction("jal func." + node.function().name());

        // teardown
        getProgram().appendInstruction("addi $sp, $sp, " + String.valueOf(node.arguments().size() * 4));
        FuncType funcType = (FuncType) node.function().type();
        if (!(funcType.returnType() instanceof VoidType)) {
            getProgram().pushInt("$v0");
        }
    }

    @Override
    public void visit(IfElseBranch node) {
        node.condition().accept(this);
        // save the condition
        getProgram().appendInstruction("lw $t1, 0($sp)");
        getProgram().appendInstruction("addi $sp, $sp, 4");
        String endLabel =  getProgram().newLabel();
        String elseLabel =  getProgram().newLabel();

        // jump to elseBlock if (condition == 0)
        getProgram().appendInstruction("beqz $t1, " + elseLabel);

        // thenBlock
        node.thenBlock().accept(this);
        getProgram().appendInstruction("jal " + endLabel);

        // elseBlock
        getProgram().appendInstruction(elseLabel + ":");
        if (node.elseBlock() != null) {
            node.elseBlock().accept(this);
        }

        getProgram().appendInstruction(endLabel + ":");
    }

    @Override
    public void visit(WhileLoop node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(Return node) {
        // push the value to $v0
        node.argument().accept(this); // assume the data is on the stack
        getProgram().popInt("$v0");
        return;
    }

    @Override
    public void visit(ast.Error node) {
        String message = "CodeGen cannot compile a " + node;
        errorBuffer.append(message);
        throw new CodeGenException(message);
    }
}
