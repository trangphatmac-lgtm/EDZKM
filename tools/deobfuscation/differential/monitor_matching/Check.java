import java.util.*;

public class Check {
    static Set set(String name, Object[] values) {
        return values == null ? null : new TrackedSet(name, values);
    }

    static String run(boolean baseline, Object[] monitors, Object[] otherMonitors,
            VerifierType[] stack, VerifierType[] otherStack, Object[] excluded,
            boolean alias, int mutation) {
        FrameState frame = baseline ? new BaselineFrame() : new CurrentFrame();
        FrameState other = alias ? frame : baseline ? new BaselineFrame() : new CurrentFrame();
        other.name = "other";
        other.stack = otherStack;
        other.heldMonitors = set("otherMonitors", otherMonitors);
        frame.name = "frame";
        frame.stack = stack;
        frame.heldMonitors = set("monitors", monitors);
        if (mutation == 1) {
            frame.onStackRead = () -> other.heldMonitors = frame.heldMonitors;
        } else if (mutation == 2 && frame.heldMonitors != null) {
            ((TrackedSet) frame.heldMonitors).onIterator = () -> other.heldMonitors = null;
        } else if (mutation == 3 && frame.heldMonitors != null) {
            ((TrackedSet) frame.heldMonitors).onIterator =
                    () -> other.heldMonitors = new TrackedSet("replacement", new Object[0]);
        }
        Set excludedValues = set("excluded", excluded);
        Probe.events.setLength(0);
        String result;
        try {
            boolean matches = baseline
                    ? ((BaselineFrame) frame).hasMatchingMonitors((BaselineFrame) other, excludedValues)
                    : ((CurrentFrame) frame).hasMatchingMonitors((CurrentFrame) other, excludedValues);
            result = "result:" + matches;
        } catch (Throwable failure) {
            result = "throws:" + failure.getClass().getName();
        }
        return result + "\n" + Probe.events;
    }

    public static void main(String[] args) {
        MonitorEnterInstruction first = new MonitorEnterInstruction(1);
        MonitorEnterInstruction second = new MonitorEnterInstruction(2);
        MonitorEnterInstruction equalButDistinct = new MonitorEnterInstruction(1);
        Object[][] monitorOptions = {
            null, {}, {first}, {second}, {first, second}, {second, first},
            {first, first}, {first, equalButDistinct}, {null}, {Integer.valueOf(5)}
        };
        VerifierType value = new VerifierType();
        VerifierType[][] stackOptions = {null, {}, {value}, {null}, {value, value}};
        Object[][] excludedOptions = {null, {}, {value}, {null}};
        long cases = 0;
        for (Object[] monitors : monitorOptions)
            for (Object[] otherMonitors : monitorOptions)
                for (VerifierType[] stack : stackOptions)
                    for (VerifierType[] otherStack : stackOptions)
                        for (Object[] excluded : excludedOptions)
                            for (boolean alias : new boolean[]{false, true})
                                for (int mutation = 0; mutation < 4; mutation++) {
                                    String expected = run(true, monitors, otherMonitors,
                                            stack, otherStack, excluded, alias, mutation);
                                    String actual = run(false, monitors, otherMonitors,
                                            stack, otherStack, excluded, alias, mutation);
                                    if (!expected.equals(actual)) {
                                        throw new AssertionError("case=" + cases
                                                + " expected=" + expected + " actual=" + actual);
                                    }
                                    cases++;
                                }
        System.out.println("hasMatchingMonitors differential result/exception/call-order checks passed: " + cases);
    }
}
