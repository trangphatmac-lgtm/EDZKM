package com.zelix;

import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.util.IntPair;
import com.zelix.klassmaster.util.SerializableDimension;
import com.zelix.klassmaster.util.SettingsObjectInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.ObjectInputStream;

public class UserPreferences implements Serializable {
    /** classpath; the field name is retained for serialized settings compatibility. */
    public String i;
    /** proGuardConfigDirectory; the field name is retained for serialized settings compatibility. */
    public String o;
    /** openNestedArchives; the field name is retained for serialized settings compatibility. */
    public boolean u;
    /** preferencesDirectory; the field name is retained for serialized settings compatibility. */
    public String b;
    /** preferencesFileName; the field name is retained for serialized settings compatibility. */
    public String a;
    /** lastSavePath; the field name is retained for serialized settings compatibility. */
    public String g;
    /** openClassesDirectory; the field name is retained for serialized settings compatibility. */
    public String c;
    /** scriptSaveDirectory; the field name is retained for serialized settings compatibility. */
    public String h;
    /** changeLogDirectory; the field name is retained for serialized settings compatibility. */
    public String d;
    /** bytecodeClasspath; the field name is retained for serialized settings compatibility. */
    public String e;
    /** useBytecodeClasspath; the field name is retained for serialized settings compatibility. */
    public boolean f = true;
    /** intSetting; the field name is retained for serialized settings compatibility. */
    public int j = 1;
    /** buildHelperSelected; the field name is retained for serialized settings compatibility. */
    public boolean k = true;
    /** crossPlatformLookAndFeel; the field name is retained for serialized settings compatibility. */
    public boolean l = true;
    /** helpWindowLocation; the field name is retained for serialized settings compatibility. */
    public IntPair m = new IntPair(0, 0);
    /** helpWindowSize; the field name is retained for serialized settings compatibility. */
    public SerializableDimension n = new SerializableDimension(400, 300);

    public synchronized void setScriptSaveDirectory(String scriptSaveDirectory) {
        this.h = scriptSaveDirectory;
    }

    public synchronized void saveToFile(final String parent, final String child) {
        final File file = new File(parent, child);
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

    public synchronized void setHelpWindowSize(int width, int height) {
        this.n = new SerializableDimension(width, height);
    }

    public synchronized String getLastSavePath() {
        return this.g;
    }

    public synchronized boolean isOpenNestedArchives() {
        return this.u;
    }

    public synchronized void savePreferences() {
        this.saveToFile(this.b, this.a);
    }

    public synchronized SerializableDimension getHelpWindowSize() {
        return this.n;
    }

    public synchronized void setProGuardConfigDirectory(String proGuardConfigDirectory) {
        this.o = proGuardConfigDirectory;
    }

    public UserPreferences() {
    }

    public synchronized void setOpenNestedArchives(boolean openNestedArchives) {
        this.u = openNestedArchives;
    }

    public synchronized int getIntSetting() {
        return this.j;
    }

    public synchronized void setBuildHelperSelected(boolean buildHelperSelected) {
        this.k = buildHelperSelected;
    }

    public synchronized String getScriptSaveDirectory() {
        return this.h;
    }

    public synchronized String getOpenClassesDirectory() {
        return this.c;
    }

    public synchronized void setIntSetting(int intSetting) {
        this.j = intSetting;
    }

    public synchronized String getProGuardConfigDirectory() {
        return this.o;
    }

    public synchronized void setChangeLogDirectory(String changeLogDirectory) {
        this.d = changeLogDirectory;
    }

    public synchronized String getChangeLogDirectory() {
        return this.d;
    }

    public synchronized boolean isCrossPlatformLookAndFeel() {
        return this.l;
    }

    public synchronized void setHelpWindowLocation(int first, int second) {
        this.m = new IntPair(first, second);
    }

    public synchronized boolean isUseBytecodeClasspath() {
        return this.f;
    }

    public synchronized boolean isBuildHelperSelected() {
        return this.k;
    }

    public synchronized void setCrossPlatformLookAndFeel(boolean crossPlatformLookAndFeel) {
        this.l = crossPlatformLookAndFeel;
    }

    public synchronized String getBytecodeClasspath() {
        return this.e;
    }

    public synchronized void setBytecodeClasspath(String bytecodeClasspath) {
        this.e = bytecodeClasspath;
    }

    public synchronized void setLastSavePath(String lastSavePath) {
        this.g = lastSavePath;
    }

    public synchronized String getClasspath() {
        return this.i;
    }

    public synchronized void setOpenClassesDirectory(String openClassesDirectory) {
        this.c = openClassesDirectory;
    }

    public synchronized void setUseBytecodeClasspath(boolean useBytecodeClasspath) {
        this.f = useBytecodeClasspath;
    }

    public synchronized void setClasspath(String classpath) {
        this.i = classpath;
    }

    public synchronized IntPair getHelpWindowLocation() {
        return this.m;
    }

    public UserPreferences(final String preferencesDirectory) {
        this.f = true;
        this.j = 1;
        this.k = true;
        this.l = true;
        this.m = new IntPair(0, 0);
        this.n = new SerializableDimension(400, 300);
        this.a = "ZKM_O.ser";
        this.b = preferencesDirectory;
        final File file = new File(preferencesDirectory, "ZKM_O.ser");
        if (file.exists() && !file.isDirectory() && file.canRead()) {
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new SettingsObjectInputStream(new FileInputStream(file));
                final UserPreferences userPreferences = (UserPreferences) objectInputStream.readObject();
                this.u = userPreferences.u;
                this.c = userPreferences.c;
                this.d = userPreferences.d;
                this.e = userPreferences.e;
                this.f = userPreferences.f;
                this.g = userPreferences.g;
                this.o = userPreferences.o;
                this.h = userPreferences.h;
                this.i = userPreferences.i;
                this.j = userPreferences.j;
                this.k = userPreferences.k;
                this.l = userPreferences.l;
                this.m = userPreferences.m;
                this.n = userPreferences.n;
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
}
