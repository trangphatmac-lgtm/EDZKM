package com.zelix.klassmaster.changelog;

public class LabeledTuple implements Comparable {
    private String label;
    public Object first;
    public Object second;
    public Object third;

    public LabeledTuple(String labelArgument, Object firstArgument, Object secondArgument, Object thirdArgument) {
        this.label = labelArgument;
        this.first = firstArgument;
        this.second = secondArgument;
        this.third = thirdArgument;
    }

    public Object getFirst() {
        return this.first;
    }

    public int compareLabels(LabeledTuple labeledTuple1) {
        return this.label.compareTo(labeledTuple1.label);
    }

    public String getLabel() {
        return this.label;
    }

    public LabeledTuple(String labelArgument, Object firstArgument) {
        this(labelArgument, firstArgument, null, null);
    }

    @Override
    public int compareTo(Object other) {
        return this.compareLabels((LabeledTuple) other);
    }

    public LabeledTuple(String labelArgument, Object firstArgument, Object secondArgument) {
        this(labelArgument, firstArgument, secondArgument, null);
    }

    public Object getSecond() {
        return this.second;
    }

    public Object getThird() {
        return this.third;
    }
}
