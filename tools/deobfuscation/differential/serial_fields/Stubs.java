import java.util.*;

class Probe {
    static StringBuilder events = new StringBuilder();
    static void event(String event) { events.append(event).append(';'); }
}
class BytecodeState {
    List instructions;
    Object getOwningClass() { Probe.event("owner"); return new ProgramClass(); }
}
class ProgramClass { String getClassName() { Probe.event("className"); return "Owner"; } }
class ZkmProcessingException extends Exception { ZkmProcessingException(String message) { super(message); } }
class FieldInfo {
    String name;
    FieldInfo(String name) { this.name = name; }
    public String toString() { return name; }
}
class ClassMemberLookup {
    boolean missing;
    FieldInfo findField(String owner, String name, String descriptor) {
        Probe.event("find:" + owner + ":" + name + ":" + descriptor);
        return missing ? null : new FieldInfo(name + ":" + descriptor);
    }
}
class ConstantPoolEntry {
    static String getWrapperPrimitiveDescriptor(String name) {
        Probe.event("wrapper:" + name);
        return "java/lang/Integer".equals(name) ? "I" : null;
    }
}
class ResolvedFieldRef extends ConstantPoolEntry {
    String owner, name, descriptor;
    ResolvedFieldRef(String owner, String name, String descriptor) {
        this.owner = owner; this.name = name; this.descriptor = descriptor;
    }
    String getReferencedClassName() { Probe.event("field.owner"); return owner; }
    String getMemberName() { Probe.event("field.name"); return name; }
    String getDescriptor() { Probe.event("field.descriptor"); return descriptor; }
    String getValueString() { Probe.event("field.value"); return owner + ":" + name + ":" + descriptor; }
}
class ResolvedMethodRef extends ConstantPoolEntry {
    String getReferencedClassName() { Probe.event("method.owner"); return "java/io/ObjectStreamField"; }
    String getMemberName() { Probe.event("method.name"); return "<init>"; }
    String getDescriptor() { Probe.event("method.descriptor"); return "(Ljava/lang/String;Ljava/lang/Class;)V"; }
}
class ResolvedClassConstant extends ConstantPoolEntry {
    String name;
    ResolvedClassConstant(String name) { this.name = name; }
    String getClassName() { Probe.event("class.constant"); return name; }
}
class ConstantUtf8 {
    String value;
    ConstantUtf8(String value) { this.value = value; }
    String getValue() { Probe.event("utf8"); return value; }
}
class ResolvedStringConstant extends ConstantPoolEntry {
    String value;
    ResolvedStringConstant(String value) { this.value = value; }
    ConstantUtf8 getValueUtf8() { Probe.event("string.constant"); return new ConstantUtf8(value); }
}
interface ConstantPoolOperand { ConstantPoolEntry getConstantPoolEntry(); }
class Instruction {
    int opcode;
    Instruction(int opcode) { this.opcode = opcode; }
    int getOpcode() { Probe.event("opcode:" + opcode); return opcode; }
}
class ConstantRefInstruction extends Instruction implements ConstantPoolOperand {
    ConstantPoolEntry entry;
    ConstantRefInstruction(int opcode, ConstantPoolEntry entry) { super(opcode); this.entry = entry; }
    public ConstantPoolEntry getConstantPoolEntry() { Probe.event("constant"); return entry; }
}
class LdcInstruction extends Instruction implements ConstantPoolOperand {
    ConstantPoolEntry entry;
    LdcInstruction(ConstantPoolEntry entry) { super(18); this.entry = entry; }
    public ConstantPoolEntry getConstantPoolEntry() { Probe.event("constant"); return entry; }
}
class BipushInstruction extends Instruction {
    byte value;
    BipushInstruction(int value) { super(16); this.value = (byte) value; }
    byte getByteValue() { Probe.event("byte"); return value; }
}
class TrackedList extends AbstractList {
    List values;
    TrackedList(List values) { this.values = values; }
    public int size() { Probe.event("instructions.size"); return values.size(); }
    public Object get(int index) { Probe.event("instructions.get:" + index); return values.get(index); }
}
