import java.util.*;

class Probe {
    static StringBuilder events = new StringBuilder();
    static void event(String value) { events.append(value).append(';'); }
}
class DescriptorState {
    static final Map PRIMITIVE_TYPE_NAMES = new HashMap() {
        public Object get(Object key) { Probe.event("primitive:" + key); return super.get(key); }
    };
    static {
        String[] descriptors = {"B", "C", "D", "F", "I", "J", "S", "Z", "V"};
        String[] names = {"byte", "char", "double", "float", "int", "long", "short", "boolean", "void"};
        for (int index = 0; index < descriptors.length; index++) PRIMITIVE_TYPE_NAMES.put(descriptors[index], names[index]);
    }
}
class ConstantPoolTag {
    static int seed;
    static int getFlowSeed() { Probe.event("seed"); return seed; }
}
class MethodSignature {
    static String getParameterPart(String descriptor) {
        Probe.event("parameters");
        return descriptor.substring(0, descriptor.lastIndexOf(")") + 1);
    }
}
class ZkmAssert {
    static void assertTrue(boolean condition, String[] message) {
        Probe.event("assert:" + condition + ":" + Arrays.toString(message));
        if (!condition) throw new AssertionError(Arrays.toString(message));
    }
}
