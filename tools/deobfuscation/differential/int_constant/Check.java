import java.util.*;
public class Check {
 static void check(int value) {
 List<Instruction> oldInstructions=new ArrayList<>(),newInstructions=new ArrayList<>();
 List<Integer> oldChanges=new ArrayList<>(),newChanges=new ArrayList<>();
 int oldLength=BaselineIntBuilder.appendIntConstant(value,oldInstructions,new ConstantPool(),oldChanges);
 int newLength=IntBuilder.appendIntConstant(value,newInstructions,new ConstantPool(),newChanges);
 if(oldLength!=newLength||!oldInstructions.toString().equals(newInstructions.toString())||!oldChanges.equals(newChanges))throw new AssertionError("value="+value);
 }
 public static void main(String[] args){
 long cases=0;
 for(int value=-1048576;value<=1048576;value++){check(value);cases++;}
 for(int value:new int[]{Integer.MIN_VALUE,Integer.MIN_VALUE+1,Integer.MAX_VALUE-1,Integer.MAX_VALUE}){check(value);cases++;}
 Random random=new Random(20261008);
 for(int i=0;i<1000000;i++){check(random.nextInt());cases++;}
 System.out.println("appendIntConstant differential instruction/pool/length checks passed: "+cases);
 }
}
