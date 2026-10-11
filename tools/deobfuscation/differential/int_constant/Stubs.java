import java.util.*;
class Instruction {
 int opcode,operand;
 Instruction() {}
 Instruction(int opcode,int operand) {this.opcode=opcode;this.operand=operand;}
 int getLength() {return opcode==19?3:operand>=-1&&operand<=5?1:operand>=-128&&operand<=127?2:3;}
 static Instruction createIntPush(int value) { return new Instruction(-1,value);}
 public String toString(){return opcode+":"+operand;}
}
class SimpleInstruction extends Instruction { SimpleInstruction(int opcode){super(opcode,0);}static SimpleInstruction forOpcode(int opcode){return new SimpleInstruction(opcode);} }
class ConstantRefInstruction extends Instruction {ConstantRefInstruction(int opcode,int operand){super(opcode,operand);}}
class ConstantPool {int getOrAddIntegerConstant_s_0(int value,List changes){changes.add(value);return value;}}
