public class Check {
 public static void main(String[] args) {
 long cases=0;
 Instruction[] sources={null,new Instruction(),new Instruction()};
 String[] names={null,"", "java/lang/Object", "java/lang/String", "[I"};
 for(boolean enabled:new boolean[]{false,true})for(boolean disabled:new boolean[]{false,true}) {
 StackFrameState.enabled=enabled;StackFrameState.disabled=disabled;
 for(Instruction source:sources)for(String name:names) {
 BaselineTracedObjectType baseline=new BaselineTracedObjectType(source,""); baseline.className=name;
 TracedObjectType current=new TracedObjectType(source,"");current.className=name;
 if(baseline.hashCode()!=current.hashCode())throw new AssertionError("hash");
 cases++;
 for(Instruction otherSource:sources)for(String otherName:names){
 BaselineTracedObjectType otherBaseline=new BaselineTracedObjectType(otherSource,"");otherBaseline.className=otherName;
 TracedObjectType otherCurrent=new TracedObjectType(otherSource,"");otherCurrent.className=otherName;
 if(baseline.equals(otherBaseline)!=current.equals(otherCurrent))throw new AssertionError("equals");
 cases++;
 }
 for(Object other:new Object[]{null,new Object(),""}) {
 if(baseline.equals(other)!=current.equals(other))throw new AssertionError("other equals");cases++;
 }
 }
 }
 System.out.println("TracedObjectType differential checks passed: "+cases);
 }
}
