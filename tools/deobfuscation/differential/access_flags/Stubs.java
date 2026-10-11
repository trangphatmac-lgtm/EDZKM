class VisitableNode {}
class UsedConstantsCollector {}
class MutableInt { MutableInt(int value) {} }
class ClassFileInputStream { int readUnsignedShort() throws java.io.IOException { return 0; } }
class ClassFileComponent {
 static VisitableNode[] nodes = new VisitableNode[4];
 static VisitableNode[] getFlowGuardNodes() { return nodes; }
 ClassFileComponent(ClassFileComponent parent) {}
 ClassFileComponent getParent() { return null; }
 int getParentKind() { return 1; }
 public void collectUsedConstants(char tag, int seed, UsedConstantsCollector collector, char mode) {}
}
