import java.util.*;

class Probe {
    static StringBuilder events = new StringBuilder();
    static void event(String event) { events.append(event).append(';'); }
}

class VerifierType {}

class MonitorEnterInstruction {
    final int key;
    MonitorEnterInstruction(int key) { this.key = key; }
    public boolean equals(Object other) {
        return other instanceof MonitorEnterInstruction
                && key == ((MonitorEnterInstruction) other).key;
    }
    public int hashCode() { return key; }
}

class FrameState {
    String name;
    Set heldMonitors;
    VerifierType[] stack;
    Runnable onStackRead;
    public VerifierType[] getStack() {
        Probe.event(name + ".stack");
        if (onStackRead != null) onStackRead.run();
        return stack;
    }
}

class TrackedSet extends AbstractSet {
    final String name;
    final Object[] values;
    Runnable onIterator;
    TrackedSet(String name, Object[] values) { this.name = name; this.values = values; }
    public int size() { Probe.event(name + ".size"); return values.length; }
    public boolean contains(Object value) {
        Probe.event(name + ".contains");
        for (Object entry : values) if (Objects.equals(entry, value)) return true;
        return false;
    }
    public Iterator iterator() {
        Probe.event(name + ".iterator");
        if (onIterator != null) onIterator.run();
        return new Iterator() {
            int index;
            public boolean hasNext() {
                Probe.event(name + ".hasNext:" + index);
                return index < values.length;
            }
            public Object next() {
                Probe.event(name + ".next:" + index);
                return values[index++];
            }
        };
    }
}
