package com.zelix.klassmaster.classfile.hierarchy;

import com.zelix.klassmaster.archive.InputFileLocation;
import com.zelix.klassmaster.changelog.TraceClassNotFoundException;
import com.zelix.klassmaster.classfile.ClassFileInputStream;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.exceptions.ClassLookupException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.exceptions.ZkmProcessingException;
import com.zelix.klassmaster.util.ArrayEnumeration;
import com.zelix.klassmaster.util.ListenerRegistry;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.NodeVisitor;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ObservableModel;
import com.zelix.klassmaster.util.ThreeKeyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;

public class ClasspathClassResolver implements NodeVisitor {
    public static final Boolean CACHED_TRUE = Boolean.TRUE;
    public static final Boolean CACHED_FALSE = Boolean.FALSE;
    public ClassPathResolver classPathResolver;
    public HashMap classCache;
    public ListenerRegistry listenerRegistry = new ListenerRegistry();
    public ObservableHolder statusHolder = new ObservableHolder();
    public TwoKeyMap superclassCache = new TwoKeyMap();
    public TwoKeyMap interfaceCache = new TwoKeyMap();
    public ZkmClasspath classpath;
    public final boolean caseSensitive;

    public final boolean implementsInterface(String key, String keyArgument) throws ZkmException, IOException {
        if (key.equals("java/lang/Object")) {
            return false;
        }

        Boolean boolean1 = (Boolean) this.interfaceCache.getValue(keyArgument, key);
        if (boolean1 != null) {
            return boolean1;
        }

        ProgramClass programClass1 = this.loadProgramClass(key);
        ArrayEnumeration arrayEnumeration = new ArrayEnumeration(programClass1.getInterfaceNames());

        while (arrayEnumeration.hasMoreElements()) {
            String keyValue = (String) arrayEnumeration.nextElement();
            if (keyValue.equals(keyArgument)) {
                this.interfaceCache.putValue(keyArgument, key, CACHED_TRUE);
                return true;
            }

            boolean bl = this.implementsInterface(keyValue, keyArgument);
            if (bl) {
                this.interfaceCache.putValue(keyArgument, key, CACHED_TRUE);
                return true;
            }
        }

        String superclassName = programClass1.getSuperclassName();
        boolean bl1 = this.implementsInterface(superclassName, keyArgument);
        this.interfaceCache.putValue(keyArgument, key, bl1 ? CACHED_TRUE : CACHED_FALSE);
        return bl1;
    }

    public ClasspathClassResolver(ZkmClasspath zkmClasspath, boolean caseSensitive) throws ZkmException, IOException {
        this.classpath = zkmClasspath;
        this.caseSensitive = caseSensitive;
        this.classpath.addObserver(this);
        this.resetClassPathResolver();
    }

    @Override
    public void handleObservedChange(ObservableModel observableModel1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        this.resetClassPathResolver();
    }

    public final boolean isSubclassOf(String key, String keyArgument) throws ZkmException, IOException {
        if (key.equals("java/lang/Object")) {
            return false;
        } else {
            Boolean boolean1 = (Boolean) this.superclassCache.getValue(keyArgument, key);
            if (boolean1 != null) {
                return boolean1;
            } else {
                String superclassName = this.loadProgramClass(key).getSuperclassName();
                if (superclassName.equals(keyArgument)) {
                    this.superclassCache.putValue(keyArgument, key, CACHED_TRUE);
                    return true;
                } else if (superclassName.equals("java/lang/Object")) {
                    this.superclassCache.putValue(keyArgument, key, CACHED_FALSE);
                    return false;
                } else {
                    boolean subclassOf = this.isSubclassOf(superclassName, keyArgument);
                    this.superclassCache.putValue(keyArgument, key, subclassOf ? CACHED_TRUE : CACHED_FALSE);
                    return subclassOf;
                }
            }
        }
    }

    public void clearCaches() {
        if (this.classCache != null) {
            this.classCache.clear();
        }

        this.superclassCache.clear();
        this.interfaceCache.clear();
        this.listenerRegistry.removeAllObservers();
        if (this.classPathResolver != null) {
            this.classPathResolver.close();
        }

        if (this.classpath != null) {
            this.classpath.closeJrtFileSystem();
        }
    }

    public void resetClassPathResolver() throws ZkmException, IOException {
        if (this.classPathResolver != null) {
            this.classPathResolver.close();
        }

        this.classPathResolver = new ClassPathResolver(this.classpath.getClasspath());
        this.classCache = ZkmUtils.createHashMap();
    }

    public ProgramClass loadProgramClass(String string) throws ZkmException, IOException {
        String key = string;
        HashMap hashMap;
        if (key.startsWith("[")) {
            key = "java/lang/Object";
            hashMap = this.classCache;
        } else {
            hashMap = this.classCache;
        }

        ProgramClass programClass1 = (ProgramClass) hashMap.get(key);
        if (programClass1 != null) {
            return programClass1;
        }

        InputFileLocation inputFileLocation = this.classPathResolver.findClassFile(key, false);
        int ba;
        InputStream inputStream1;
        if (inputFileLocation == null) {
            if (!this.classpath.isJrtAvailable()) {
                throw new TraceClassNotFoundException(
                        key, "Class '" + ZkmUtils.slashesToDots(key) + "' not found. Check the classpath option and reopen classes. (4)"
                );
            }

            try {
                ObservableHolder observableHolder = new ObservableHolder();
                byte[] byteArray = this.classpath.readJrtClassBytes(key, observableHolder);
                if (byteArray == null) {
                    throw new TraceClassNotFoundException(
                            key, "Class '" + ZkmUtils.slashesToDots(key) + "' not found. Check the classpath option and reopen classes. (3)"
                    );
                }

                inputFileLocation = new InputFileLocation((Path) observableHolder.getValue(), this.classpath);
                inputStream1 = new ByteArrayInputStream(byteArray);
                ba = byteArray.length;
            } catch (IOException iOException2) {
                throw new TraceClassNotFoundException(
                        key, "Class '" + ZkmUtils.slashesToDots(key) + "' not found. Check the classpath option and reopen classes. (1)"
                );
            }
        } else {
            MutableInt mutableInt = new MutableInt(0);

            try {
                inputStream1 = inputFileLocation.openInputStream(mutableInt);
                ba = mutableInt.getValue();
            } catch (IOException iOException1) {
                throw new TraceClassNotFoundException(key, "'" + key + "' : File error (E) : '" + iOException1 + "'");
            }
        }

        try {
            if (inputStream1 != null) {
                ClassFileInputStream classFileInputStream = ClassFileInputStream.fromStream(inputStream1, ba);
                StringWriter stringWriter1 = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter1);
                StringWriter stringWriter = new StringWriter();
                new PrintWriter(stringWriter);
                ThreeKeyMultiMap threeKeyMultiMap = new ThreeKeyMultiMap(4, 3, 5, false);

                try {
                    programClass1 = new ProgramClass(
                            classFileInputStream, inputFileLocation, this.statusHolder, this.listenerRegistry, printWriter, threeKeyMultiMap
                    );
                } catch (ZkmProcessingException zkmProcessingException) {
                    throw new ClassLookupException(zkmProcessingException.getMessage());
                }

                if (!key.equals(programClass1.getClassName())) {
                    String name = inputFileLocation.getName();
                    String archivePath = inputFileLocation.getArchivePath();
                    if (archivePath != null) {
                        throw new ClassLookupException(
                                "File '"
                                        + name
                                        + "' in '"
                                        + archivePath
                                        + "' contains the class '"
                                        + programClass1.getDottedClassName()
                                        + "' rather than '"
                                        + ZkmUtils.slashesToDots(key)
                                        + "' as expected."
                        );
                    } else {
                        throw new ClassLookupException(
                                "File '"
                                        + name
                                        + "' contains the class '"
                                        + programClass1.getDottedClassName()
                                        + "' rather than '"
                                        + ZkmUtils.slashesToDots(key)
                                        + "' as expected."
                        );
                    }
                } else {
                    this.classCache.put(key, programClass1);
                    return programClass1;
                }
            } else {
                throw new ClassLookupException("Class '" + ZkmUtils.slashesToDots(key) + "' not found. Check the classpath option and reopen classes. (2)");
            }
        } catch (IOException iOException) {
            throw new ClassLookupException("'" + ZkmUtils.slashesToDots(key) + "' : File error (F) : " + iOException);
        }
    }
}
