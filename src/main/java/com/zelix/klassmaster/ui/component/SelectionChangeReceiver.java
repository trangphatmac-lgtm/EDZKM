package com.zelix.klassmaster.ui.component;

public interface SelectionChangeReceiver {
    void handleSelectionDelta(int[] integerArray, int[] integerArrayArgument);

    void handleAccessSelection(int ba);
}
