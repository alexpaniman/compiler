package execute;

import executor.NativeExecutor;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Stack;

import static execute.Operand.*;
import static java.lang.Math.pow;

public class VirtualMachine {
    private int index;
    private Object[] program;
    private Stack<Frame> frames;
    private Stack<Object> stack;
    private Map<Object, Object> variables;
    private NativeExecutor nativeExecutor;

    public VirtualMachine(NativeExecutor nativeExecutor, Object[] program) {
        this.index = 0;
        this.program = program;
        this.nativeExecutor = nativeExecutor;

        this.frames = new Stack<>();
        this.frames.push(new Frame(-1));

        this.stack = new Stack<>();
        this.variables = frames.peek().vars();
    }


    private Object calculate(Object obj) throws VirtualMachineException {
        Operand operator = (Operand) obj;
        Object first = stack.pop();
        Class classFirst = first.getClass();
        Object second = stack.pop();
        Class classSecond = second.getClass();
        if (classFirst == String.class || classSecond == String.class) {
            switch (operator) {
                case ADD:
                    return second.toString() + first.toString();
                case MUL:
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < (int) first; i++)
                        sb.append(second);
                    return sb.toString();
                default:
                    throw new VirtualMachineException("Unsupported operator");
            }
        }
        if (classFirst == Double.class || classSecond == Double.class) {
            double d1;
            if (classSecond == Double.class)
                d1 = (double) second;
            else
                d1 = (int) second;

            double d2;
            if (classFirst == Double.class)
                d2 = (double) first;
            else
                d2 = (int) first;
            switch (operator) {
                case EQL:
                    return d1 == d2 ? 1 : 0;
                case ADD:
                    return d1 + d2;
                case SUB:
                    return d1 - d2;
                case MUL:
                    return d1 * d2;
                case DIV:
                    return d1 / d2;
                case MOD:
                    return d1 % d2;
                case POW:
                    return pow(d1, d2);
                case LT:
                    return d1 < d2 ? 1 : 0;
                case BT:
                    return d1 > d2 ? 1 : 0;
                case LE:
                    return d1 <= d2 ? 1 : 0;
                case BE:
                    return d1 >= d2 ? 1 : 0;
            }
        }
        int i1 = (int) second;
        int i2 = (int) first;
        switch (operator) {
            case EQL:
                return i1 == i2 ? 1 : 0;
            case ADD:
                return i1 + i2;
            case SUB:
                return i1 - i2;
            case MUL:
                return i1 * i2;
            case DIV:
                return i1 / i2;
            case MOD:
                return i1 % i2;
            case POW:
                return pow(i1, i2);
            case AND:
                return i1 & i2;
            case XOR:
                return i1 ^ i2;
            case OR:
                return i1 | i2;
            case LT:
                return i1 < i2 ? 1 : 0;
            case BT:
                return i1 > i2 ? 1 : 0;
            case LE:
                return i1 <= i2 ? 1 : 0;
            case BE:
                return i1 >= i2 ? 1 : 0;
        }
        throw new IllegalArgumentException();
    }

    public boolean execute() throws VirtualMachineException {
        if (program[index] == PUSH) {
            stack.push(program[index + 1]);
            index += 2;
        } else if (program[index] == POP) {
            stack.pop();
            index += 1;
        } else if (program[index] == STORE) {
            variables.put(program[index + 1], stack.pop());
            index += 2;
        } else if (program[index] == FETCH) {
            stack.push(variables.get(program[index + 1]));
            index += 2;
        } else if (program[index] == JMP)
            index = (int) program[index + 1];
        else if (program[index] == JZ)
            if ((int) stack.pop() == 0)
                index = (int) program[index + 1];
            else
                index += 2;
        else if (program[index] == JNZ)
            if ((int) stack.pop() != 0)
                index = (int) program[index + 1];
            else
                index += 2;

        else if (program[index] == INVOKE) {
            Frame frame = new Frame(index + 2);

            frames.push(frame);
            variables = frame.vars();

            index = (int) program[index + 1];
        } else if (program[index] == RET) {
            index = frames.pop().returnIndex();

            if (index == -1)
                return false;

            variables = frames.peek().vars();
        } else if (program[index] == NATIVE) {
            try {
                stack.push(nativeExecutor.invoke((int) program[index + 1], stack));
            } catch (NoSuchMethodException exc) {
                throw new VirtualMachineException("Can't find or access native method!");
            }
            index += 2;
        } else if (program[index] == NOT) {
            stack.push((int) stack.pop() == 0 ? 1 : 0);
            index += 1;
        } else {
            stack.push(calculate(program[index]));
            index += 1;
        }
        return true;
    }

    public void executeAll() throws VirtualMachineException {
        while (true) {
            if (!execute())
                break;
        }
    }

    public String status() {
        String status = "STATUS:\n";
        status += "\tEXECUTION: " + (index >= program.length || index == -1 ? "COMPLETED" : "INCOMPLETE") + "\n";
        status += "\tFRAMES: " + (frames.empty() ? "ALL FRAMES DROPPED" : frames.size() + " FRAME IS ALIVE") + "\n";
        status += "\tSTACK: " + stack;
        return status;
    }
}