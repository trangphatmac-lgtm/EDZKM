import java.lang.reflect.*;
public class Check {
 public static void main(String[] args) throws Exception {
  long cases = 0;
  for (boolean guardPresent : new boolean[]{true,false}) {
   ClassFileComponent.nodes = guardPresent ? new VisitableNode[4] : null;
   for (int flags = 0; flags < 65536; flags++) {
    for (int kind : new int[]{Integer.MIN_VALUE,-1,0,1,2,3,4,Integer.MAX_VALUE}) {
     for (boolean extended : new boolean[]{false,true}) {
      for (boolean negated : new boolean[]{false,true}) {
       String expected = BaselineAccessFlags.formatModifiers(flags, kind, extended, negated);
       String actual = AccessFlags.formatModifiers(flags, kind, extended, negated);
       if (!expected.equals(actual)) throw new AssertionError("flags="+flags+" kind="+kind+" guard="+guardPresent+" expected="+expected+" actual="+actual);
       cases++;
      }
     }
    }
   }
   for(Method method : AccessFlags.class.getDeclaredMethods()) {
    if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount()==1 && method.getParameterTypes()[0]==int.class) {
     Method baseline = BaselineAccessFlags.class.getDeclaredMethod(method.getName(),int.class);
     for (int flags=0; flags<65536; flags++) {
      Object expected=baseline.invoke(null,flags),actual=method.invoke(null,flags);
      if(!expected.equals(actual)) throw new AssertionError(method+" flags="+flags);
      cases++;
     }
    }
   }
  }
  System.out.println("AccessFlags differential checks passed: "+cases);
 }
}
