package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.ui.dialog.EscapeClosableFrame;

import java.awt.Component;
import javax.swing.JList;

public abstract class ListItemWrapper {
    public Object item;

    public abstract void applyRenderStyle(Component component1, JList jList, boolean bl);

    public Object getItem() {
        return this.item;
    }

    @Override
    public String toString() {
        return this.item.toString();
    }

    @Override
    public int hashCode() {
        return this.item.hashCode();
    }

    public ListItemWrapper(Object itemArgument) {
        this.item = itemArgument;
    }

    @Override
    public boolean equals(Object other) {
        int invertedControlValue = EscapeClosableFrame.getInvertedControlValue();
        boolean bl = other instanceof ListItemWrapper;
        if (invertedControlValue == 0) {
            if (bl) {
                return this.item.equals(((ListItemWrapper) other).item);
            }

            bl = false;
        }

        return bl;
    }
}
