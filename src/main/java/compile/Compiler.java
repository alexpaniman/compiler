package compile;

import execute.Operand;
import lexer.Token;
import nodes.*;

import java.util.*;

import static execute.Operand.*;

public class Compiler {
    private List<Object> program;
    private Map<String, Integer> variables;
    private Map<String, Integer> functions;
    private Map<String, Integer> nativeFunctions;
    private int count;
    private int nativeCount;

    public Compiler() {
        this.program = new ArrayList<>();
        this.variables = new HashMap<>();
        this.functions = new HashMap<>();
        this.nativeFunctions = new HashMap<>();
        this.count = 0;
        this.nativeCount = 0;
    }

    private void gen(Object... ops) {
        program.addAll(Arrays.asList(ops));
    }

    private void binary(Operand operand, BinaryOperator node) {
        compile(node.first());
        compile(node.second());
        gen(operand);
    }

    private void math(BinaryOperator node) {
        Token token = node.operator();
        if (token == Token.NOT_EQL) {
            binary(Operand.EQL, node);
            gen(NOT);
        } else if (token == Token.XOR)
            binary(Operand.XOR, node);
        else if (token == Token.EQL)
            binary(Operand.EQL, node);
        else if (token == Token.AND)
            binary(Operand.AND, node);
        else if (token == Token.SUM)
            binary(Operand.ADD, node);
        else if (token == Token.SUB)
            binary(Operand.SUB, node);
        else if (token == Token.MUL)
            binary(Operand.MUL, node);
        else if (token == Token.DIV)
            binary(Operand.DIV, node);
        else if (token == Token.MOD)
            binary(Operand.MOD, node);
        else if (token == Token.POW)
            binary(Operand.POW, node);
        else if (token == Token.LT)
            binary(Operand.LT, node);
        else if (token == Token.BT)
            binary(Operand.BT, node);
        else if (token == Token.LE)
            binary(Operand.LE, node);
        else if (token == Token.BE)
            binary(Operand.BE, node);
        else if (token == Token.OR)
            binary(Operand.OR, node);
    }

    private void math(UnaryOperator node) {
        Token token = node.operator();
        if (token == Token.NOT) {
            compile(node.operand());
            gen(NOT);
        }
    }

    private void compile(INode node) {
        if (node instanceof Program)
            compile(((Program) node).program());
        else if (node instanceof BinaryOperator)
            math((BinaryOperator) node);
        else if (node instanceof UnaryOperator)
            math((UnaryOperator) node);
        else if (node instanceof Block)
            for (INode n : ((Block) node).nodes())
                compile(n);
        else if (node instanceof Variable)
            gen(FETCH, variables.get(((Variable) node).name()));
        else if (node instanceof Constant)
            gen(PUSH, ((Constant) node).value());
        else if (node instanceof Assign) {
            Assign assign = (Assign) node;
            compile(assign.expression());
            gen(STORE, variables.computeIfAbsent(assign.variable(), n -> count++));
        } else if (node instanceof For) {
            For forLoop = (For) node;
            compile(forLoop.initializer());
            int loop = program.size();
            compile(forLoop.condition());
            gen(JZ, 0);
            int index = program.size() - 1;
            compile(forLoop.body());
            compile(forLoop.iterator());
            gen(JMP, loop);
            program.set(index, program.size());
        } else if (node instanceof While) {
            While whileLoop = (While) node;
            int loop = program.size();
            compile(whileLoop.condition());
            gen(JZ, 0);
            int index = program.size() - 1;
            compile(whileLoop.body());
            gen(JMP, loop);
            program.set(index, program.size());
        } else if (node instanceof DoWhile) {
            DoWhile doWhile = (DoWhile) node;
            compile(doWhile.body());
            int loop = program.size();
            compile(doWhile.condition());
            gen(JZ, 0);
            int index = program.size() - 1;
            compile(doWhile.body());
            gen(JMP, loop);
            program.set(index, program.size());
        } else if (node instanceof ShortIf) {
            ShortIf shortIf = (ShortIf) node;
            compile(shortIf.condition());
            gen(JZ, 0);
            int index = program.size() - 1;
            compile(shortIf.thenNode());
            program.set(index, program.size());
        } else if (node instanceof ExtendedIf) {
            ExtendedIf extendedIf = (ExtendedIf) node;
            compile(extendedIf.condition());
            gen(JZ, 0);
            int index = program.size() - 1;
            compile(extendedIf.thenNode());
            gen(JMP, 0);
            program.set(index, program.size());
            index = program.size() - 1;
            compile(extendedIf.elseNode());
            program.set(index, program.size());
        } else if (node instanceof DefineFunction) {
            DefineFunction defineFunc = (DefineFunction) node;
            gen(JMP, 0);
            functions.put(defineFunc.name(), program.size());
            int index = program.size() - 1;
            for (INode n : defineFunc.variables()) {
                Variable var = (Variable) n;
                gen(STORE, variables.computeIfAbsent(var.name(), nodeC -> count++));
            }
            if (defineFunc.body() instanceof Block)
                compile(defineFunc.body());
            else {
                compile(defineFunc.body());
                gen(RET);
            }
            program.set(index, program.size());
        } else if (node instanceof Return) {
            Return ret = (Return) node;
            if (ret.expression() != null)
                compile(ret.expression());
            else
                gen(PUSH, 0);
            gen(RET);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            INode[] nodes = call.variables();
            for (int i = nodes.length - 1; i >= 0; i--)
                compile(nodes[i]);
            if (functions.containsKey(call.name()))
                gen(INVOKE, functions.get(call.name()));
            else {
                String name = call.name();
                if (nativeFunctions.containsKey(name))
                    gen(NATIVE, nativeFunctions.get(name));
                else
                    gen(NATIVE, nativeFunctions.computeIfAbsent(name, nodeC -> nativeCount ++));
            }
            if (!call.useReturn())
                gen(POP);
        }
    }

    public void compileProgram(INode main) {
        compile(main);
        gen(RET);
    }

    public Object[] program() {
        return program.toArray(new Object[0]);
    }

    public Map<String, Integer> nativeFunctions() {
        return nativeFunctions;
    }
}
