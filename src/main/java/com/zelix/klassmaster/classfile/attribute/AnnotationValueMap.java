package com.zelix.klassmaster.classfile.attribute;

import java.util.HashMap;

public class AnnotationValueMap {
    public HashMap valuesByName = new HashMap(13);
    public RuntimeInvisibleAnnotationsAttribute sourceAttribute;

    public RuntimeInvisibleAnnotationsAttribute getSourceAttribute() {
        return this.sourceAttribute;
    }

    public AnnotationValueMap(RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotationsAttribute) {
        this.sourceAttribute = runtimeInvisibleAnnotationsAttribute;
    }

    public String getValue(Object key) {
        return (String) this.valuesByName.get(key);
    }

    public void putValue(Object key, Object value) {
        this.valuesByName.put(key, value);
    }

    public boolean hasValue(Object key) {
        return this.valuesByName.containsKey(key);
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
