package com.zelix.klassmaster.config;

import com.zelix.annotation.ExceptionObfuscationPolicy;
import com.zelix.annotation.FlowObfuscationPolicy;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.util.SettingsObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.io.ObjectInputStream;

public class ObfuscateOptions implements Serializable {
    // Preserve the existing serialized identity while recovering non-persistent member names.
    private static final long serialVersionUID = -8579173474158073371L;

    public static final Map FLOW_LEVELS_BY_NAME = new HashMap(13);
    public static final Map FLOW_NAMES_BY_LEVEL = new HashMap(13);
    public static final Map EXCEPTION_LEVELS_BY_NAME = new HashMap(13);
    public static final Map EXCEPTION_NAMES_BY_LEVEL = new HashMap(13);
    public static final Map LEVEL_NAMES_BY_POLICY = new HashMap(13);
    /** newFieldNameFile; the field name is retained for serialized settings compatibility. */
    public String di;
    /** inputChangeLogs; the field name is retained for serialized settings compatibility. */
    public ChangeLogInputFile[] g;
    /** newMethodNameFile; the field name is retained for serialized settings compatibility. */
    public String dB;
    /** newNamesPrefix; the field name is retained for serialized settings compatibility. */
    public String x;
    /** newClassNameFile; the field name is retained for serialized settings compatibility. */
    public String dE;
    /** methodParameterChangesPackage; the field name is retained for serialized settings compatibility. */
    public String dN;
    /** assumedRuntimeVersion; the field name is retained for serialized settings compatibility. */
    public String an;
    public Integer at;
    /** autoReflectionPackage; the field name is retained for serialized settings compatibility. */
    public String q;
    public transient PrintWriter changeLogWriter;
    /** defaultCollapsedPackage; the field name is retained for serialized settings compatibility. */
    public String t;
    /** referenceObfuscationPackage; the field name is retained for serialized settings compatibility. */
    public String ai;
    /** changeLogEncoding; the field name is retained for serialized settings compatibility. */
    public String v;
    public transient boolean loaded;
    /** autoReflectionHash; the field name is retained for serialized settings compatibility. */
    public String D;
    /** newPackageNameFile; the field name is retained for serialized settings compatibility. */
    public String dv;
    public int b = 1;
    /** genericsInfoMode; the field name is retained for serialized settings compatibility. */
    public int u = 0;
    public boolean c = false;
    public boolean e = false;
    public boolean d = true;
    /** outputChangeLogFile; the field name is retained for serialized settings compatibility. */
    public String f = "ChangeLog.txt";
    /** lineNumbersMode; the field name is retained for serialized settings compatibility. */
    public int i = 0;
    /** localVariablesMode; the field name is retained for serialized settings compatibility. */
    public int B = 0;
    public int am = 0;
    /** aggressiveMethodRenaming; the field name is retained for serialized settings compatibility. */
    public boolean w = false;
    /** legalIdentifiers; the field name is retained for serialized settings compatibility. */
    public boolean E = true;
    public boolean dr = true;
    /** preverify; the field name is retained for serialized settings compatibility. */
    public boolean j = true;
    /** makeClassesPublic; the field name is retained for serialized settings compatibility. */
    public boolean XN = false;
    /** keepBalancedLocks; the field name is retained for serialized settings compatibility. */
    public boolean ar = false;
    /** mixedCaseNamesMode; the field name is retained for serialized settings compatibility. */
    public MixedCaseNamesMode F = MixedCaseNamesMode.IF_IN_ARCHIVE;
    /** uniqueClassNames; the field name is retained for serialized settings compatibility. */
    public boolean ay = false;
    /** uniqueMethodNames; the field name is retained for serialized settings compatibility. */
    public boolean dV = false;
    /** methodParameterChangesMode; the field name is retained for serialized settings compatibility. */
    public int aC = 0;
    /** hideFieldNames; the field name is retained for serialized settings compatibility. */
    public boolean k = false;
    /** hideStaticMethodNames; the field name is retained for serialized settings compatibility. */
    public boolean l = false;
    /** flowObfuscationLevel; the field name is retained for serialized settings compatibility. */
    public int o = 1;
    public int d7 = 0;
    /** stringEncryptionLevel; the field name is retained for serialized settings compatibility. */
    public int as = 4;
    /** integerEncryptionLevel; the field name is retained for serialized settings compatibility. */
    public int dZ = 0;
    /** longEncryptionLevel; the field name is retained for serialized settings compatibility. */
    public int dC = 0;
    public boolean p = true;
    /** exceptionObfuscationLevel; the field name is retained for serialized settings compatibility. */
    public int C = 1;
    /** collapsePackages; the field name is retained for serialized settings compatibility. */
    public boolean s = false;
    /** randomizeNames; the field name is retained for serialized settings compatibility. */
    public boolean z = false;
    /** deriveGroupingsFromInputChangeLog; the field name is retained for serialized settings compatibility. */
    public boolean y = false;
    public boolean A = true;
    /** referenceObfuscationMode; the field name is retained for serialized settings compatibility. */
    public int aj = 0;
    /** referenceStructuresMode; the field name is retained for serialized settings compatibility. */
    public int m = 1;
    /** autoReflectionMode; the field name is retained for serialized settings compatibility. */
    public int r = 1;

    public static String getLevelNameForPolicy(String key) {
        return (String) LEVEL_NAMES_BY_POLICY.get(key);
    }

    public ObfuscateOptions() {
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public void saveToFile(final String parent) {
        final File file = new File(parent, "ZKM_SO.ser");
        if (file.exists() && (file.isDirectory() || !file.canWrite())) {
            return;
        }
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(this);
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

    public static int parseFlowLevel(String key) {
        return (Integer) FLOW_LEVELS_BY_NAME.get(key);
    }

    public static String getFlowLevelName(Integer integer) {
        return (String) FLOW_NAMES_BY_LEVEL.get(integer);
    }

    public static int parseExceptionLevel(String key) {
        return (Integer) EXCEPTION_LEVELS_BY_NAME.get(key);
    }

    static {
        FLOW_LEVELS_BY_NAME.put("none", 0);
        FLOW_LEVELS_BY_NAME.put("light", 1);
        FLOW_LEVELS_BY_NAME.put("normal", 2);
        FLOW_LEVELS_BY_NAME.put("aggressive", 3);
        FLOW_LEVELS_BY_NAME.put("extraAggressive", 4);
        FLOW_NAMES_BY_LEVEL.put(0, "none");
        FLOW_NAMES_BY_LEVEL.put(1, "light");
        FLOW_NAMES_BY_LEVEL.put(2, "normal");
        FLOW_NAMES_BY_LEVEL.put(3, "aggressive");
        FLOW_NAMES_BY_LEVEL.put(4, "extraAggressive");
        EXCEPTION_LEVELS_BY_NAME.put("none", 0);
        EXCEPTION_LEVELS_BY_NAME.put("light", 1);
        EXCEPTION_LEVELS_BY_NAME.put("heavy", 2);
        EXCEPTION_NAMES_BY_LEVEL.put(0, "none");
        EXCEPTION_NAMES_BY_LEVEL.put(1, "light");
        EXCEPTION_NAMES_BY_LEVEL.put(2, "heavy");
        LEVEL_NAMES_BY_POLICY.put(FlowObfuscationPolicy.NONE.name(), "none");
        LEVEL_NAMES_BY_POLICY.put(FlowObfuscationPolicy.LIGHT.name(), "light");
        LEVEL_NAMES_BY_POLICY.put(FlowObfuscationPolicy.NORMAL.name(), "normal");
        LEVEL_NAMES_BY_POLICY.put(FlowObfuscationPolicy.AGGRESSIVE.name(), "aggressive");
        LEVEL_NAMES_BY_POLICY.put(ExceptionObfuscationPolicy.HEAVY.name(), "heavy");
    }

    public ObfuscateOptions(final String parent, final String child) {
        this.b = 1;
        this.u = 0;
        this.c = false;
        this.e = false;
        this.d = true;
        this.f = "ChangeLog.txt";
        this.i = 0;
        this.B = 0;
        this.am = 0;
        this.w = false;
        this.E = true;
        this.dr = true;
        this.j = true;
        this.XN = false;
        this.ar = false;
        this.F = MixedCaseNamesMode.IF_IN_ARCHIVE;
        this.ay = false;
        this.dV = false;
        this.aC = 0;
        this.k = false;
        this.l = false;
        this.o = 1;
        this.d7 = 0;
        this.as = 4;
        this.dZ = 0;
        this.dC = 0;
        this.p = true;
        this.C = 1;
        this.s = false;
        this.z = false;
        this.y = false;
        this.A = true;
        this.aj = 0;
        this.m = 1;
        this.r = 1;
        final File file = new File(parent, child);
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new SettingsObjectInputStream(new FileInputStream(file));
                final ObfuscateOptions obfuscateOptions = (ObfuscateOptions) objectInputStream.readObject();
                this.b = obfuscateOptions.b;
                this.F = obfuscateOptions.F;
                this.ay = obfuscateOptions.ay;
                this.dV = obfuscateOptions.dV;
                this.aC = obfuscateOptions.aC;
                this.u = obfuscateOptions.u;
                this.c = obfuscateOptions.c;
                this.e = obfuscateOptions.e;
                this.d = obfuscateOptions.d;
                this.g = obfuscateOptions.g;
                this.f = obfuscateOptions.f;
                this.i = obfuscateOptions.i;
                this.B = obfuscateOptions.B;
                this.am = obfuscateOptions.am;
                this.w = obfuscateOptions.w;
                this.E = obfuscateOptions.E;
                this.dr = obfuscateOptions.dr;
                this.j = obfuscateOptions.j;
                this.XN = obfuscateOptions.XN;
                this.ar = obfuscateOptions.ar;
                this.s = obfuscateOptions.s;
                this.t = obfuscateOptions.t;
                this.k = obfuscateOptions.k;
                this.l = obfuscateOptions.l;
                this.o = obfuscateOptions.o;
                this.d7 = obfuscateOptions.d7;
                this.as = obfuscateOptions.as;
                this.dZ = obfuscateOptions.dZ;
                this.dC = obfuscateOptions.dC;
                this.p = obfuscateOptions.p;
                this.x = obfuscateOptions.x;
                this.z = obfuscateOptions.z;
                this.y = obfuscateOptions.y;
                this.A = obfuscateOptions.A;
                this.r = obfuscateOptions.r;
                this.q = obfuscateOptions.q;
                this.D = obfuscateOptions.D;
                this.C = obfuscateOptions.C;
                this.aj = obfuscateOptions.aj;
                this.m = obfuscateOptions.m;
                this.ai = obfuscateOptions.ai;
                this.an = obfuscateOptions.an;
                this.at = obfuscateOptions.at;
                this.dv = obfuscateOptions.dv;
                this.dE = obfuscateOptions.dE;
                this.di = obfuscateOptions.di;
                this.dB = obfuscateOptions.dB;
                this.loaded = true;
                try {
                    objectInputStream.close();
                } catch (final IOException ioException) {
                }
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
    }

    public static String getExceptionLevelName(Integer integer) {
        return (String) EXCEPTION_NAMES_BY_LEVEL.get(integer);
    }
}
