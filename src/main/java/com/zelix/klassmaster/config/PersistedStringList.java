package com.zelix.klassmaster.config;

import com.zelix.klassmaster.util.ZkmAssert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class PersistedStringList implements Serializable {
    // Preserve the existing serialized identity while recovering non-persistent member names.
    private static final long serialVersionUID = -3783585091275300098L;

    public transient boolean saved;
    public Map l = new LinkedHashMap();

    public final Enumeration enumerateEntries() {
        ZkmAssert.assertNotNull(this.l, "Unexpected null value (A) : " + this.saved);
        Set set1 = this.l.keySet();
        ZkmAssert.assertNotNull(set1, "Unexpected null value (B) : " + this.saved);
        return Collections.enumeration(set1);
    }

    public final int getEntryCount() {
        return this.l.size();
    }

    public void setEntries(List list1) {
        this.l.clear();

        for (int elementIndex = 0; elementIndex < list1.size(); elementIndex++) {
            String string = (String) list1.get(elementIndex);
            this.l.put(string, string);
        }
    }

    public final List getEntryList() {
        return new ArrayList(this.l.keySet());
    }

    public final void saveToFile(final String parent, final String child) {
        final File file = new File(parent, child);
        if (file.exists() && (file.isDirectory() || !file.canWrite())) {
            return;
        }
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(this);
            this.saved = true;
            try {
                objectOutputStream.close();
            } catch (final IOException ioException) {
            }
        } catch (final IOException ioExceptionValue) {
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (final IOException ioExceptionValue2) {
                }
            }
        }
    }

    public final boolean isSaved() {
        return this.saved;
    }
}
