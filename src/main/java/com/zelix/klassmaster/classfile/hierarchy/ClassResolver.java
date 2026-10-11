package com.zelix.klassmaster.classfile.hierarchy;

import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.config.IgnoreMissingReferencesSpec;
import com.zelix.klassmaster.exceptions.ZkmException;

import java.io.IOException;

public class ClassResolver {
    public ClasspathClassLoader classLoader;

    public ClassFileBase getVersionedClass(String key, Integer integer, String string1) throws ZkmException, IOException {
        return this.findVersionedClass(key, integer, string1);
    }

    public ClassFileBase getClassFile(String key) throws ZkmException, IOException {
        return this.findClassFile(key, true, (String) null);
    }

    public ClassFileBase getVersionedClass(String key, Integer integer, String string1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1) throws ZkmException, IOException {
        return this.resolveClass(key, integer, true, string1, ignoreMissingReferencesSpec1);
    }

    public ClassFileBase getClassFile(String key, String string1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1) throws ZkmException, IOException {
        return this.resolveClass(key, null, true, string1, ignoreMissingReferencesSpec1);
    }

    public ClassFileBase findClassFile(String key) throws ZkmException, IOException {
        return this.findClassFile(key, false, (String) null);
    }

    public void releaseLoader() {
        if (this.classLoader != null) {
            this.classLoader.releaseReaders();
        }
    }

    public ClassFileBase findClassFile(String key, boolean bl, String string1) throws ZkmException, IOException {
        return this.resolveClass(key, null, bl, string1, null);
    }

    public ClassFileBase resolveClass(String key, Integer integer, boolean bl, String string1, IgnoreMissingReferencesSpec ignoreMissingReferencesSpec1) throws ZkmException, IOException {
        ClassFileBase classFileBase = ClassHierarchyNode.findClassFile(key);
        if (classFileBase != null) {
            return integer != null && classFileBase.hasVersionedVariants() ? classFileBase.selectVersionForRelease(integer) : classFileBase;
        } else {
            return this.classLoader.loadClassFile(key, integer, bl, string1, ignoreMissingReferencesSpec1);
        }
    }

    public ClassResolver(ClasspathClassLoader classpathClassLoader1) {
        this.classLoader = classpathClassLoader1;
    }

    public ClassFileBase findVersionedClass(String key, Integer integer, String string1) throws ZkmException, IOException {
        return this.resolveClass(key, integer, true, string1, null);
    }

    public ClassFileBase getClassFile(String key, String string1) throws ZkmException, IOException {
        return this.findClassFile(key, true, string1);
    }

    public ClassFileBase getVersionedClass(String key, Integer integer) throws ZkmException, IOException {
        return this.findVersionedClass(key, integer, (String) null);
    }
}
