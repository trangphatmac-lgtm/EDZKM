public class Check {
    static String run(boolean baseline, Instruction instruction, int seed, char mode, boolean empty, boolean missingCollector) {
        BytecodeState state = baseline ? new BaselineConstants() : new CurrentConstants();
        state.instructions = new TrackedList(instruction, empty);
        UsedConstantsCollector collector = missingCollector ? null : new UsedConstantsCollector();
        Probe.events.setLength(0);
        String result;
        try {
            if (baseline) ((BaselineConstants) state).collectUsedConstants((char) 0, seed, collector, mode);
            else ((CurrentConstants) state).collectUsedConstants((char) 0, seed, collector, mode);
            result = "returned";
        } catch (Throwable failure) {
            result = "throws:" + failure.getClass().getName();
        }
        return result + "\n" + Probe.events;
    }
    public static void main(String[] args) {
        long cases = 0;
        for (int opcode = -1; opcode <= 256; opcode++) {
            for (Instruction instruction : new Instruction[]{null, new Instruction(opcode), new PoolInstruction(opcode),
                    new ConstantRefInstruction(opcode), new LdcInstruction(opcode)}) {
                for (int seed : new int[]{-1, 0, 1})
                    for (char mode : new char[]{0, 1, Character.MAX_VALUE})
                        for (boolean empty : new boolean[]{false, true})
                            for (boolean missingCollector : new boolean[]{false, true}) {
                                String expected = run(true, instruction, seed, mode, empty, missingCollector);
                                String actual = run(false, instruction, seed, mode, empty, missingCollector);
                                if (!expected.equals(actual)) throw new AssertionError("opcode=" + opcode + " seed=" + seed
                                        + " mode=" + (int) mode + " expected=" + expected + " actual=" + actual);
                                cases++;
                            }
            }
        }
        System.out.println("constant usage differential result/exception/call-order checks passed: " + cases);
    }
}
