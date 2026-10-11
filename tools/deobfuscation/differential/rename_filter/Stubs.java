import java.util.*;
class FilterState {
 public List interfaceNames,parameterTypes,throwsNames,packageNames,searchNames;
 public AccessFlagsSpec classAccessFlags,memberAccessFlags;
 public String archivePattern,className,superclassName,classAnnotation,fieldName,fieldType,methodName,linkedMethodName,memberAnnotation;
 public boolean packageNameOnly,excludeContainingPackage,excludeContainingClass,includeSignatureClasses;
 public ASTContainingClause containingClause;
}
class Probe {static StringBuilder events=new StringBuilder();static void event(String value){events.append(value).append(';');}}
class ZkmScriptSimpleNode {static int[] keys;static int readIndex;static int getFlowControlKey(){int value=keys[Math.min(readIndex++,keys.length-1)];Probe.event("guard="+value);return value;}}
class AccessFlagsSpec {String id,text;boolean required;AccessFlagsSpec(String id,String text,boolean required){this.id=id;this.text=text;this.required=required;}boolean isPackageRequired(){Probe.event(id+".package");return required;}String toSpecString(){Probe.event(id+".spec");return text;}}
class ASTContainingClause {String toScriptText(){Probe.event("containing.script");return "containing test";}}
class TrackedList extends AbstractList {String id;Object[] values;TrackedList(String id,Object[] values){this.id=id;this.values=values;}public int size(){Probe.event(id+".size");return values.length;}public Object get(int index){Probe.event(id+".get:"+index);return values[index];}}
