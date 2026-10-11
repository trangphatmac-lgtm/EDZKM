import java.util.*;

public class Check {
    static String run(boolean baseline, boolean parameters, String descriptor, boolean validate, boolean dotted) {
        Probe.events.setLength(0);
        String result;
        try {
            Object value = parameters
                    ? baseline ? BaselineDescriptors.parseParameterTypes(descriptor, validate)
                               : CurrentDescriptors.parseParameterTypes(descriptor, validate)
                    : baseline ? BaselineDescriptors.descriptorToJavaType(descriptor, validate, dotted)
                               : CurrentDescriptors.descriptorToJavaType(descriptor, validate, dotted);
            result = "result:" + value;
        } catch (Throwable failure) {
            result = "throws:" + failure.getClass().getName();
        }
        return result + "\n" + Probe.events;
    }
    static long verify(boolean parameters, String descriptor, boolean allowUnchecked) {
        long cases = 0;
        for (int seed : new int[]{0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            ConstantPoolTag.seed = seed;
            for (boolean validate : new boolean[]{false, true}) {
                if (parameters && !validate && !allowUnchecked && seed == 0) continue;
                for (boolean dotted : new boolean[]{false, true}) {
                    String expected = run(true, parameters, descriptor, validate, dotted);
                    String actual = run(false, parameters, descriptor, validate, dotted);
                    if (!expected.equals(actual)) throw new AssertionError("descriptor=" + descriptor
                            + " seed=" + seed + " validate=" + validate + " parameters=" + parameters
                            + " expected=" + expected + " actual=" + actual);
                    cases++;
                }
            }
        }
        return cases;
    }
    public static void main(String[] args) {
        long cases = 0;
        for (String descriptor : new String[]{null, "", "(", ")", "()", "()V", "(L;)V", "([)V",
                "([L;)V", "(Lmissing)V", "[", "[[[", "L", "L;", "Lx;", "Lx", "Q", "(Q)V", "x()I"}) {
            cases += verify(true, descriptor, false);
            cases += verify(false, descriptor, true);
        }
        Random random = new Random(20261008);
        String[] types = {"I", "V", "J", "S", "B", "C", "Z", "F", "D", "Q", "Ljava/lang/String;", "Lfoo/Bar;"};
        for (int sample = 0; sample < 20000; sample++) {
            StringBuilder descriptor = new StringBuilder("(");
            int count = random.nextInt(8);
            for (int index = 0; index < count; index++) {
                descriptor.append("[".repeat(random.nextInt(5)));
                descriptor.append(types[random.nextInt(types.length)]);
            }
            descriptor.append(')').append("[".repeat(random.nextInt(5))).append(types[random.nextInt(types.length)]);
            cases += verify(true, descriptor.toString(), true);
            cases += verify(false, descriptor.toString(), true);
        }
        System.out.println("descriptor differential result/exception/call-order checks passed: " + cases);
    }
}
