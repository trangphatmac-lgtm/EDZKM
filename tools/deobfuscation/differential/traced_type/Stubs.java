interface TrackedValue {
String getNormalizedName(); boolean isPending(); String getStringValue(); boolean isConstant(); int getPendingInstructionIndex(); String getDisplayText(); boolean isKnown(); String getCombinedValueKey();
}
class Instruction {}
class StackFrameState {
static boolean enabled, disabled;
static boolean isStrictMergeEnabled() { return enabled; }
static boolean isStrictMergeDisabled() { return disabled; }
}
