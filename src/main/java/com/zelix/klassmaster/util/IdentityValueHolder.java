package com.zelix.klassmaster.util;

import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class IdentityValueHolder extends ObservableHolder {
    public IdentityValueHolder() throws ZkmException, IOException {
        super(null);
    }

    @Override
    public int hashCode() {
        return this.value != null ? System.identityHashCode(this.value) : 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IdentityValueHolder)) {
            return false;
        } else {
            return this.value != null ? this.value == ((IdentityValueHolder) other).getValue() : other == null;
        }
    }

    public IdentityValueHolder(Object valueArgument) throws ZkmException, IOException {
        super(valueArgument);
    }
}
