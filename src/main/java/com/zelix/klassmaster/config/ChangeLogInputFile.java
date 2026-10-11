package com.zelix.klassmaster.config;

import java.io.Reader;
import java.io.Serializable;

public class ChangeLogInputFile implements Serializable {
    // Preserve the existing serialized identity while recovering non-persistent member names.
    private static final long serialVersionUID = -8635664898563946187L;

    public transient Reader reader;
    /** fileName; the field name is retained for serialized settings compatibility. */
    public final String a;

    public Reader getReader() {
        return this.reader;
    }

    public String getFileName() {
        return this.a;
    }

    public ChangeLogInputFile(String fileName) {
        this.a = fileName;
    }

    public void setReader(Reader reader1) {
        this.reader = reader1;
    }
}
