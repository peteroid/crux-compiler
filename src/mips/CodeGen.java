package mips;

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
        getProgram().pushAddress("$t0");
    }

    @Override
    public void visit(LiteralBool node) {
        String boolString = node.value() == LiteralBool.Value.TRUE? "1" : "0";
        getProgram().appendInstruction("li $t0, " + boolString);
        getProgram().pushBool("$t0");
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
            node.rightSide().accept(this);
            getProgram().popInt("$t2");
            getProgram().popInt("$t1");
            getProgram().appendInstruction("add $t1, $t1, $t2");
            getProgram().pushInt("$t1");
        } else if (tc.getType(node) instanceof FloatType) {
            node.leftSide().accept(this);
            node.rightSide().accept(this);
            getProgram().popFloat("$f2");
            getProgram().popFloat("$f1");
            getProgram().appendInstruction("add.s $f1, $f1, $f2");
            getProgram().pushFloat("$f1");
        } else {
            throw new RuntimeException("Unknown Type");
        }
    }

    @Override
    public void visit(Subtraction node) {
        Type operandType = tc.getType(node);
        if (operandType instanceof IntType) {
            node.leftSide().accept(this);
            node.rightSide().accept(this);
            getProgram().popInt("$t2");
            getProgram().popInt("$t1");
            getProgram().appendInstruction("sub $t1, $t1, $t2");
            getProgram().pushInt("$t1");
        } else if (operandType instanceof FloatType) {
            node.leftSide().accept(this);
            node.rightSide().accept(this);
            getProgram().popFloat("$f2");
            getProgram().popFloat("$f1");
            getProgram().appendInstruction("sub.s $f1, $f1, $f2");
            getProgram().pushFloat("$f1");
        } else {
            throw new RuntimeException("Unknown Type");
        }
    }

    @Override
    public void visit(Multiplication node) {
        Type operandType = tc.getType(node);
        if (operandType instanceof IntType) {
            node.leftSide().accept(this);
            node.rightSide().accept(this);
            getProgram().popInt("$t2");
            getProgram().popInt("$t1");
            getProgram().appendInstruction("mul $t2, $t1, $t2");
            getProgram().pushInt("$t2");
        } else if (operandType instanceof FloatType) {
//            node.leftSide().accept(this);
//            getProgram().popFloat("$f1");
//            node.rightSide().accept(this);
//            getProgram().popFloat("$f2");
//            getProgram().appendInstruction("sub.s $f1, $f1, $f2");
//            getProgram().pushFloat("$f1");
        } else {
            throw new RuntimeException("Unknown Type");
        }
    }

    @Override
    public void visit(Division node) {
        throw new RuntimeException("Implement this");
    }

    @Override
    public void visit(LogicalAnd node) {
        node.leftSide().accept(this);
        getProgram().popBool("$t2");
        node.rightSide().accept(this);
        getProgram().popBool("$t3");

        getProgram().appendInstruction("and $t2, $t2, $t3");
        getProgram().pushBool("$t2");
    }

    @Override
    public void visit(LogicalOr node) {
        // fixme: not test
        node.leftSide().accept(this);
        getProgram().popBool("$t2");
        node.rightSide().accept(this);
        getProgram().popBool("$t3");

        getProgram().appendInstruction("or $t2, $t2, $t3");
        getProgram().pushBool("$t2");
    }
    
    @Override
    public void visit(LogicalNot node) {
        // fixme: not test
        node.expression().accept(this);
        getProgram().popBool("$t2");

        getProgram().appendInstruction("li $t3, -1");
        getProgram().appendInstruction("xor $t2, $t2, $t3");
        getProgram().pushBool("$t2");
    }

    @Override
    public void visit(Comparison node) {
        String trueLabel = getProgram().newLabel();
        String joinLabel = getProgram().newLabel();
        Type operandType = tc.getType((Command) node.leftSide());

        if (operandType instanceof IntType) {
            String branchInstruction = null;
            switch (node.operation()) {
                case EQ:
                    branchInstruction = "beq";
                    break;
                case NE:
                    branchInstruction = "bne";
                    break;
                case LE:
                    branchInstruction = "blez";
                    break;
                case LT:
                    branchInstruction = "bltz";
                    break;
                case GE:
                    branchInstruction = "bgez";
                    break;
                case GT:
                    branchInstruction = "bgtz";
                    break;
                default:
                    throw new RuntimeException("Unknown Operation");
            }

            node.leftSide().accept(this);
            getProgram().popInt("$t2");
            node.rightSide().accept(this);
            getProgram().popInt("$t3");

            if (node.operation() == Comparison.Operation.EQ || node.operation() == Comparison.Operation.NE) {
                getProgram().appendInstruction(branchInstruction + " $t2, $t3 " + trueLabel);
            } else {
                getProgram().appendInstruction("sub $t2, $t2, $t3");
                getProgram().appendInstruction(branchInstruction + " $t2 " + trueLabel);
            }
            getProgram().appendInstruction("li $t0, 0");
            getProgram().appendInstruction("jal " + joinLabel);
            getProgram().appendInstruction(trueLabel + ":");
            getProgram().appendInstruction("li $t0, 1");
        } else if (operandType instanceof FloatType) {
            node.leftSide().accept(this);
            getProgram().popFloat("$f2");
            node.rightSide().accept(this);
            getProgram().popFloat("$f3");

            if (node.operation() == Comparison.Operation.EQ || node.operation() == Comparison.Operation.NE) {
                getProgram().appendInstruction("c.eq.s $f2, $f3");
                getProgram().appendInstruction("bc1t " + trueLabel);
                getProgram().appendInstruction("li $t0, " + (node.operation() == Comparison.Operation.EQ? "0" : "1"));
                getProgram().appendInstruction("jal " + joinLabel);
                getProgram().appendInstruction(trueLabel + ":");
                getProgram().appendInstruction("li $t0, " + (node.operation() == Comparison.Operation.EQ? "1" : "0"));
            } else {
                // float condition: c.COND.FMT fs, ft
                // float branch: bclt TARGET
                String floatCondition = "c." + node.operation().name().toLowerCase() + ".s ";
                getProgram().appendInstruction(floatCondition + " $f2, $f3");

                getProgram().appendInstruction("bc1t " + trueLabel);
                getProgram().appendInstruction("li $t0, 0");
                getProgram().appendInstruction("jal " + joinLabel);
                getProgram().appendInstruction(trueLabel + ":");
                getProgram().appendInstruction("li $t0, 1");
            }
        } else {
            throw new RuntimeException("Unknown Type");
        }
        getProgram().appendInstruction(joinLabel + ":");
        getProgram().pushBool("$t0");
    }

    @Override
    public void visit(Dereference node) {
        node.expression().accept(this);
        getProgram().popAddress("$t0");
        getProgram().appendInstruction("lw $t2, 0($t0)");
        //getProgram().appendInstruction("sw $t1, 0($sp)");
        getProgram().pushInt("$t2"); // fixme: can guarantee as an int?
    }

    @Override
    public void visit(Index node) {
        // fixme: only support 2D array
        // $t0[$sp]
        // push the address and amount on the stack
        // structure: (-) amountN ... amount1 address (+)

        node.amount().accept(this);

        if (node.base() instanceof Index) {
            // push the index for multi-dimensional array
            node.base().accept(this);
        } else {
            node.base().accept(this); // address
            getProgram().popAddress("$t0"); // address
            getProgram().popInt("$t3"); // index

            ArrayType arrType = (ArrayType) tc.getType((Command) node.base());

            // support 2D
            if (arrType.base() instanceof ArrayType) {
                int scale = arrType.extent();

                // build the offset
                getProgram().popInt("$t2"); // index
                getProgram().appendInstruction("li $t1, " + String.valueOf(scale));
                getProgram().appendInstruction("mul $t2, $t2, $t1");
                getProgram().appendInstruction("add $t3, $t3, $t2");
            }

            // set up index
            getProgram().appendInstruction("li $t4, 4");
            getProgram().appendInstruction("mul $t3, $t3, $t4");

            // set up entry address
            getProgram().appendInstruction("add $t0, $t0, $t3");
            getProgram().pushAddress("$t0");
        }
    }

    @Override
    public void visit(Assignment node) {
        node.source().accept(this); // data on stack
        Type sourceType = tc.getType((Command) node.source());
        if (sourceType instanceof IntType) {
            node.destination().accept(this); // address on stack
            getProgram().popAddress("$t0");
            getProgram().popInt("$t2");
            getProgram().appendInstruction("sw $t2, 0($t0)");
        } else if (sourceType instanceof FloatType) {
            node.destination().accept(this); // address on stack
            getProgram().popAddress("$t0");
            getProgram().popFloat("$f1");
            getProgram().appendInstruction("swc1 $f1, 0($t0)");
        } else {
            throw new CodeGenException("Unknown type in assignment: " + sourceType.toString());
        }
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
        getProgram().popBool("$t1");
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
        String beforeLabel = getProgram().newLabel();
        String afterLabel = getProgram().newLabel();

        getProgram().appendInstruction(beforeLabel + ":");
        node.condition().accept(this);
        getProgram().popBool("$t2");
        getProgram().appendInstruction("bne $t2, 1, " + afterLabel);
        node.body().accept(this);
        getProgram().appendInstruction("jal " + beforeLabel);
        getProgram().appendInstruction(afterLabel + ":");

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
