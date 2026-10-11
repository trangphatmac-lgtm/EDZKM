import java.util.*;

class Probe {
    static StringBuilder events = new StringBuilder();
    static void event(String event) { events.append(event).append(';'); }
}
class BytecodeState { List instructions; }
class ClassFileComponent { static Object getFlowGuardNodes() { Probe.event("guard"); return null; } }
class ConstantPoolEntry {}
interface ConstantPoolOperand { ConstantPoolEntry getConstantPoolEntry(); }
class Instruction {
    int opcode;
    Instruction(int opcode) { this.opcode = opcode; }
    int getOpcode() { Probe.event("opcode"); return opcode; }
}
class PoolInstruction extends Instruction implements ConstantPoolOperand {
    PoolInstruction(int opcode) { super(opcode); }
    public ConstantPoolEntry getConstantPoolEntry() { Probe.event("constant"); return null; }
}
class ConstantRefInstruction extends PoolInstruction { ConstantRefInstruction(int opcode) { super(opcode); } }
class LdcInstruction extends PoolInstruction { LdcInstruction(int opcode) { super(opcode); } }
class UsedConstantsCollector {
    void markUsed(ConstantPoolEntry constant, Object instruction, Object owner) { Probe.event("mark"); }
}
class TrackedList extends AbstractList {
    Instruction instruction;
    int reads;
    boolean empty;
    TrackedList(Instruction instruction, boolean empty) { this.instruction = instruction; this.empty = empty; }
    public int size() { Probe.event("size"); return empty ? 0 : 1; }
    public Object get(int index) {
        Probe.event("get:" + index);
        // A negative control seed retries the same instruction forever; an injected
        // failure verifies identical retry behavior without hanging the test.
        if (++reads == 4) throw new IllegalStateException("read limit");
        return instruction;
    }
}
