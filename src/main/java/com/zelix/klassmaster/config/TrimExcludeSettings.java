package com.zelix.klassmaster.config;

import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.util.SettingsObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.io.ObjectInputStream;

public class TrimExcludeSettings extends WizardAppSettings implements Serializable {
    public static TrimExcludeSettings loadFromFile(final String parent) {
        final File file = new File(parent, "ZKM_TEX.ser");
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new SettingsObjectInputStream(new FileInputStream(file));
                final TrimExcludeSettings trimExcludeSettings = (TrimExcludeSettings) objectInputStream.readObject();
                if (trimExcludeSettings.l == null) {
                    trimExcludeSettings.l = new LinkedHashMap();
                    trimExcludeSettings.saved = true;
                } else {
                    trimExcludeSettings.saved = true;
                }
                final TrimExcludeSettings trimExcludeSettings2 = trimExcludeSettings;
                try {
                    objectInputStream.close();
                } catch (final IOException ioException) {
                }
                return trimExcludeSettings2;
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
        return new TrimExcludeSettings();
    }
}
