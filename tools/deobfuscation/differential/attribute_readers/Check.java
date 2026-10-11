import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Check {
    static String failureText(Throwable failure, Set<Throwable> seen) {
        if (!seen.add(failure)) return "repeated";
        String result = failure.getClass().getName();
        if (failure instanceof IOException || failure instanceof AssertionError || failure instanceof IllegalStateException)
            result += ":" + failure.getMessage();
        if (failure.getCause() != null) result += " cause=" + failureText(failure.getCause(), seen);
        for (Throwable suppressed : failure.getSuppressed()) result += " suppressed=" + failureText(suppressed, seen);
        return result;
    }
    static String run(boolean baseline, int kind, int[] input, int failRead, int closeMode, boolean nullStream) {
        Settings.kind = kind; Settings.input = input; Settings.failRead = failRead;
        Settings.closeMode = closeMode; Settings.nullStream = nullStream; Settings.constructed = null;
        Probe.events.setLength(0);
        StringWriter log = new StringWriter(); PrintWriter diagnostics = new PrintWriter(log);
        ClassFileBase owner = new ClassFileBase(); ClassFileInputStream stream = new ClassFileInputStream();
        ListMultimap refs = new ListMultimap(); String result;
        try {
            if (kind == 0) {
                if (baseline) new BaselineNestMembersAttribute(owner, 1, "NestMembers", stream, refs);
                else new CurrentNestMembersAttribute(owner, 1, "NestMembers", stream, refs);
            } else if (kind == 1) {
                if (baseline) new BaselineModulePackagesAttribute(owner, 1, "ModulePackages", stream, refs);
                else new CurrentModulePackagesAttribute(owner, 1, "ModulePackages", stream, refs);
            } else if (kind == 2) {
                if (baseline) new BaselinePermittedSubclassesAttribute(owner, 1, "PermittedSubclasses", stream, refs, refs, diagnostics);
                else new CurrentPermittedSubclassesAttribute(owner, 1, "PermittedSubclasses", stream, refs, refs, diagnostics);
            } else {
                if (baseline) new BaselineParameterAnnotationsAttribute(owner, 1, "Annotations", stream, refs, diagnostics, "Annotations");
                else new CurrentParameterAnnotationsAttribute(owner, 1, "Annotations", stream, refs, diagnostics, "Annotations");
            }
            result = "returned";
        } catch (Throwable failure) {
            result = failureText(failure, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
        ParsedAttributeBase constructed = Settings.constructed;
        return result + " valid=" + constructed.valid + " raw=" + (constructed.rawBytes != null)
                + " log=" + log + " events=" + Probe.events;
    }
    static String collect(boolean baseline, AnnotationEntry[][] annotations, boolean valid, char mode) throws Exception {
        Settings.input = new int[]{0}; Settings.failRead = -1; Settings.closeMode = 0; Settings.nullStream = false;
        ParsedAttributeBase instance = baseline
                ? new BaselineParameterAnnotationsAttribute(new ClassFileBase(), 1, "Annotations", new ClassFileInputStream(), new ListMultimap(), new PrintWriter(new StringWriter()), "Annotations")
                : new CurrentParameterAnnotationsAttribute(new ClassFileBase(), 1, "Annotations", new ClassFileInputStream(), new ListMultimap(), new PrintWriter(new StringWriter()), "Annotations");
        instance.valid = valid;
        instance.getClass().getField("parameterAnnotations").set(instance, annotations);
        Probe.events.setLength(0); String result;
        try {
            if (baseline) ((BaselineParameterAnnotationsAttribute) instance).collectUsedConstants((char) 0, 0, new UsedConstantsCollector(), mode);
            else ((CurrentParameterAnnotationsAttribute) instance).collectUsedConstants((char) 0, 0, new UsedConstantsCollector(), mode);
            result = "returned";
        } catch (Throwable failure) {
            result = failure.getClass().getName();
        }
        return result + " " + Probe.events;
    }
    public static void main(String[] args) throws Exception {
        int[][] inputs = {{}, {0}, {1}, {1, 0}, {1, 1}, {1, 2}, {2, 1, 1}, {2, 1, 0}, {2, 1, 2},
                {1, 2, 1, 1}, {1, 2, 1, 0}, {2, 1, 0, 1, 1}, {2, 1, 1, 1, 1}};
        long cases = 0;
        for (int kind = 0; kind < 4; kind++)
            for (int[] input : inputs)
                for (int failRead = -1; failRead <= 6; failRead++)
                    for (int closeMode = 0; closeMode <= 5; closeMode++)
                        for (boolean nullStream : new boolean[]{false, true}) {
                            String expected = run(true, kind, input, failRead, closeMode, nullStream);
                            String actual = run(false, kind, input, failRead, closeMode, nullStream);
                            if (!expected.equals(actual)) throw new AssertionError("kind=" + kind + " input=" + Arrays.toString(input)
                                    + " read=" + failRead + " close=" + closeMode + " expected=" + expected + " actual=" + actual);
                            cases++;
                        }
        AnnotationEntry entry = new AnnotationEntry(true);
        AnnotationEntry[][][] arrays = {null, {}, {null}, {{}}, {{entry}}, {{entry, entry}}, {{entry}, {entry, entry}}, {{entry, entry}, {entry}}, {{null}}};
        for (AnnotationEntry[][] array : arrays)
            for (boolean valid : new boolean[]{false, true})
                for (char mode : new char[]{0, 1, Character.MAX_VALUE}) {
                    // A nonempty row with zero mode is an unchanged nonterminating path.
                    if (valid && mode == 0 && array != null && array.length > 0 && array[0] != null && array[0].length > 0) continue;
                    String expected = collect(true, array, valid, mode), actual = collect(false, array, valid, mode);
                    if (!expected.equals(actual)) throw new AssertionError("collect expected=" + expected + " actual=" + actual);
                    cases++;
                }
        System.out.println("attribute readers differential close/suppression/diagnostic/state checks passed: " + cases);
    }
}
