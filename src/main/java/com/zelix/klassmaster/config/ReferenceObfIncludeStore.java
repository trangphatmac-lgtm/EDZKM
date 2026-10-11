package com.zelix.klassmaster.config;

import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.util.SettingsObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.io.ObjectInputStream;

public class ReferenceObfIncludeStore extends PersistedStringList {
    public static ReferenceObfIncludeStore loadFromFile(final String parent) {
        final File file = new File(parent, "ZKM_OB_REF.ser");
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new SettingsObjectInputStream(new FileInputStream(file));
                final ReferenceObfIncludeStore referenceObfIncludeStore = (ReferenceObfIncludeStore) objectInputStream.readObject();
                if (referenceObfIncludeStore.l == null) {
                    referenceObfIncludeStore.l = new LinkedHashMap();
                    referenceObfIncludeStore.saved = true;
                } else {
                    referenceObfIncludeStore.saved = true;
                }
                final ReferenceObfIncludeStore referenceObfIncludeStore2 = referenceObfIncludeStore;
                try {
                    objectInputStream.close();
                } catch (final IOException ioException) {
                }
                return referenceObfIncludeStore2;
            } catch (final IOException ioExceptionValue) {
            } catch (final ClassNotFoundException classNotFoundException) {
            } catch (final ClassCastException classCastException) {
            } catch (final AssertionFailedException assertionFailedException) {
                throw assertionFailedException;
            } catch (final Throwable throwable) {
            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (final IOException ioExceptionValue2) {
                    }
                }
            }
        }
        return new ReferenceObfIncludeStore();
    }

    private ReferenceObfIncludeStore() {
    }
}
