package com.zelix.klassmaster.changelog;

import java.util.Comparator;

public class ChangeLogEntryComparator implements Comparator {
    public final ChangeLogWriter writer;

    @Override
    public int compare(Object object, Object object1) {
        return this.compareEntries((LabeledTuple) object, (LabeledTuple) object1);
    }

    public int compareEntries(LabeledTuple labeledTuple, LabeledTuple labeledTuple1) {
        String label = labeledTuple.getLabel();
        String labelValue = labeledTuple1.getLabel();
        String string2 = label.substring(label.indexOf(" ") + 1);
        String string3 = labelValue.substring(labelValue.indexOf(" ") + 1);
        int ba = string2.compareTo(string3);
        return ba == 0 ? label.compareTo(labelValue) : ba;
    }

    public ChangeLogEntryComparator(ChangeLogWriter changeLogWriter1) {
        this.writer = changeLogWriter1;
    }
}
