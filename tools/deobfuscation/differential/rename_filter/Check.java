import java.util.*;import java.lang.reflect.*;
public class Check {
 static Random random=new Random(20261008);
 static String[] texts={null,"","*","Foo","@Annotation","(complex)","trailing "};
 static String text(){return texts[random.nextInt(texts.length)];}
 static List list(String id){int kind=random.nextInt(8);return kind==0?null:new TrackedList(id,kind==1?new Object[]{}:kind==2?new Object[]{null}:kind==3?new Object[]{Integer.valueOf(5)}:kind<6?new Object[]{"A"}:new Object[]{"A","B"});}
 static String run(FilterState model,int[] keys) {
 ZkmScriptSimpleNode.keys=keys;ZkmScriptSimpleNode.readIndex=0;Probe.events.setLength(0);
 String result;
 try {result="value:"+(model instanceof BaselineFilter?((BaselineFilter)model).toScriptText():((CurrentFilter)model).toScriptText());}
 catch(Throwable failure){result="throws:"+failure.getClass().getName();}
 return result+"\n"+Probe.events;
 }
 public static void main(String[] args)throws Exception {
 long cases=0;
 int[][] keyOptions={{0},{1},{-1},{0,1},{1,0},{1,0,1},{1,1,0}};
 for(int trial=0;trial<100000;trial++){
 BaselineFilter baseline=new BaselineFilter();CurrentFilter current=new CurrentFilter();
 for(Field field:FilterState.class.getFields()) {
 Object value;
 if(field.getType()==String.class)value=text();
 else if(field.getType()==boolean.class)value=random.nextBoolean();
 else if(field.getType()==List.class)value=list(field.getName());
 else if(field.getType()==AccessFlagsSpec.class)value=random.nextInt(4)==0?null:new AccessFlagsSpec(field.getName(),text(),random.nextBoolean());
 else value=random.nextBoolean()?null:new ASTContainingClause();
 field.set(baseline,value);field.set(current,value);
 }
 for(int[] keys:keyOptions){
 String expected=run(baseline,keys),actual=run(current,keys);
 if(!expected.equals(actual))throw new AssertionError("trial="+trial+" keys="+Arrays.toString(keys)+"\nexpected="+expected+"\nactual="+actual);
 cases++;
 }
 }
 System.out.println("RenameFilterModel differential output/exception/call-order checks passed: "+cases);
 }
}
