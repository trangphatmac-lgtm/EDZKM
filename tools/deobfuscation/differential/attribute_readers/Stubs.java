import java.util.*;
import java.io.*;

class Probe {
    static StringBuilder events = new StringBuilder();
    static void event(String value) { events.append(value).append(';'); }
}
class Settings {
    static int[] input;
    static int failRead, closeMode, kind;
    static boolean nullStream;
    static ParsedAttributeBase constructed;
}
class ClassFileInputStream {
    int reads, closes, cursor;
    IOException repeatedCloseFailure = new IOException("close");
    int read(byte[] bytes) { Probe.event("read.raw"); return bytes.length; }
    static ClassFileInputStream fromBytes(byte[] bytes, boolean option) {
        Probe.event("fromBytes");
        return Settings.nullStream ? null : new ClassFileInputStream();
    }
    int next() throws IOException {
        if (++reads == Settings.failRead) throw new IOException("read");
        if (cursor >= Settings.input.length) throw new EOFException("end");
        return Settings.input[cursor++];
    }
    int readUnsignedShort() throws IOException { Probe.event("short"); return next(); }
    int readUnsignedByte() throws IOException { Probe.event("byte"); return next(); }
    void close() throws IOException {
        Probe.event("close:" + ++closes);
        if (Settings.closeMode == 1 && closes == 1 || Settings.closeMode == 2 && closes == 2
                || Settings.closeMode == 3) throw new IOException("close:" + closes);
        if (Settings.closeMode == 4) throw new IllegalStateException("close:" + closes);
        if (Settings.closeMode == 5) throw repeatedCloseFailure;
    }
}
class ClassFileComponent {
    static VisitableNode[] getFlowGuardNodes() { Probe.event("guard"); return null; }
    ConstantPoolEntry getConstantPoolEntry(int index) {
        Probe.event("pool:" + index);
        if (index == 0) return null;
        if (index == 1) return Settings.kind == 1 ? new ResolvedPackageConstant() : new ResolvedClassConstant();
        return new ConstantPoolEntry();
    }
    ClassFileComponent getOwningClass() { Probe.event("owningClass"); return this; }
    String getLocationName() { Probe.event("location"); return "location"; }
}
class ClassFileBase extends ClassFileComponent {
    void setHasPermittedSubclasses() { Probe.event("permitted.flag"); }
}
class ParsedAttributeBase extends ClassFileComponent {
    int length;
    boolean valid = true;
    byte[] rawBytes;
    Object nameConstant = "name";
    ClassFileComponent parent;
    String attributeName;
    ParsedAttributeBase(ClassFileComponent parent, int index, String name, ClassFileInputStream input, ListMultimap refs) {
        Probe.event("super"); this.parent = parent; attributeName = name;
        length = Settings.input.length * 2; Settings.constructed = this;
    }
    Object getParent() { Probe.event("parent"); return parent; }
    String getDisplayLocationName() { Probe.event("displayLocation"); return "display"; }
    String getAttributeName() { Probe.event("attributeName"); return attributeName; }
}
class AnnotationAttributeBase extends ParsedAttributeBase {
    AnnotationAttributeBase(ClassFileComponent parent, int index, String name, ClassFileInputStream input, ListMultimap refs) {
        super(parent, index, name, input, refs);
    }
}
class ClassFileFormatException extends IOException { ClassFileFormatException(String text) { super(text); } }
class ConstantPoolEntry {}
class ResolvedClassConstant extends ConstantPoolEntry { String getValueString() { Probe.event("class.value"); return "Class"; } }
class ResolvedPackageConstant extends ConstantPoolEntry { String getValueString() { Probe.event("package.value"); return "Package"; } }
class ListMultimap { void addValue(Object key, Object value) { Probe.event("reference"); } }
class ZkmUtils { static boolean allUnique(Object[] values) { Probe.event("unique"); return values.length < 2; } }
class ZkmAssert { static void assertTrue(boolean condition, String[] message) { Probe.event("assert"); if (!condition) throw new AssertionError(Arrays.toString(message)); } }
class VisitableNode {}
class AnnotationEntry extends VisitableNode {
    boolean valid;
    AnnotationEntry(boolean valid) { this.valid = valid; }
    static AnnotationEntry read(Object owner, ClassFileInputStream input, ListMultimap refs) throws IOException {
        Probe.event("annotation.read"); return new AnnotationEntry(input.readUnsignedByte() != 0);
    }
    boolean isValid() { Probe.event("annotation.valid"); return valid; }
    String getErrorMessage(int tag, int seed, int mode) { Probe.event("annotation.error"); return "invalid annotation"; }
    void collectUsedConstants(char tag, int seed, UsedConstantsCollector collector, char mode) { Probe.event("annotation.collect"); }
}
class UsedConstantsCollector { void markUsed(Object constant, Object user, Object owner) { Probe.event("mark"); } }
