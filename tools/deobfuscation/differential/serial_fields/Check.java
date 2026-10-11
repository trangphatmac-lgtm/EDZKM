import java.util.*;

public class Check {
    static Instruction putFields() {
        return new ConstantRefInstruction(179,
                new ResolvedFieldRef("Owner", "serialPersistentFields", "[Ljava/io/ObjectStreamField;"));
    }
    static List<Instruction> validFields(int count, boolean primitive) {
        List<Instruction> code = new ArrayList<>();
        code.add(new BipushInstruction(count));
        code.add(new ConstantRefInstruction(189, new ResolvedClassConstant("java/io/ObjectStreamField")));
        for (int index = 0; index < count; index++) {
            code.add(new Instruction(89));
            code.add(new BipushInstruction(index));
            code.add(new ConstantRefInstruction(187, new ResolvedClassConstant("java/io/ObjectStreamField")));
            code.add(new Instruction(89));
            code.add(new LdcInstruction(new ResolvedStringConstant("field" + index)));
            code.add(primitive
                    ? new ConstantRefInstruction(178, new ResolvedFieldRef("java/lang/Integer", "TYPE", "Ljava/lang/Class;"))
                    : new LdcInstruction(new ResolvedClassConstant("Ljava/lang/String;")));
            code.add(new ConstantRefInstruction(183, new ResolvedMethodRef()));
            code.add(new Instruction(83));
        }
        code.add(putFields());
        return code;
    }
    static String run(boolean baseline, List<Instruction> code, boolean missing) {
        BytecodeState state = baseline ? new BaselineSerialFields() : new CurrentSerialFields();
        state.instructions = new TrackedList(code);
        List<FieldInfo> fields = new ArrayList<>();
        ClassMemberLookup lookup = new ClassMemberLookup(); lookup.missing = missing;
        Probe.events.setLength(0);
        String result;
        try {
            if (baseline) ((BaselineSerialFields) state).collectSerialPersistentFields(fields, lookup);
            else ((CurrentSerialFields) state).collectSerialPersistentFields(fields, lookup);
            result = "returned";
        } catch (Throwable failure) {
            result = "throws:" + failure.getClass().getName();
            if (failure instanceof ZkmProcessingException) result += ":" + failure.getMessage();
        }
        return result + "\n" + fields + "\n" + Probe.events;
    }
    public static void main(String[] args) {
        Random random = new Random(20261008);
        Instruction[] replacements = {null, new Instruction(0), new Instruction(178), new Instruction(189),
                new LdcInstruction(new ResolvedStringConstant("type")),
                new LdcInstruction(new ResolvedClassConstant("LType;")),
                new ConstantRefInstruction(178, new ResolvedFieldRef("java/lang/Object", "TYPE", "Ljava/lang/Class;")),
                new ConstantRefInstruction(187, new ResolvedClassConstant("Other")),
                new ConstantRefInstruction(178, new ResolvedFieldRef("java/io/ObjectStreamClass", "NO_FIELDS", "[Ljava/io/ObjectStreamField;")),
                new BipushInstruction(-1), new Instruction(3), new Instruction(8),
                new ConstantRefInstruction(183, new ResolvedMethodRef()), putFields()};
        long cases = 0;
        for (int sample = 0; sample < 30000; sample++) {
            List<Instruction> code = validFields(random.nextInt(7), random.nextBoolean());
            int mutations = sample < 100 ? 0 : random.nextInt(5);
            for (int mutation = 0; mutation < mutations; mutation++) {
                code.set(random.nextInt(code.size()), replacements[random.nextInt(replacements.length)]);
            }
            if (sample % 17 == 0) code.clear();
            if (sample % 19 == 0) Collections.reverse(code);
            for (boolean missing : new boolean[]{false, true}) {
                String expected = run(true, code, missing), actual = run(false, code, missing);
                if (!expected.equals(actual)) throw new AssertionError("sample=" + sample
                        + " expected=" + expected + " actual=" + actual);
                cases++;
            }
        }
        System.out.println("serialPersistentFields differential result/exception/call-order checks passed: " + cases);
    }
}
