package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.util.ZkmUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class TempFileManager {
    private static Map tempFileOriginalNames;
    public static String tempDirPath;
    public static File tempDir;

    public static void deleteStaleTempFiles() {
        try {
            long ba = System.currentTimeMillis();
            String[] strings = tempDir.list(new PrefixSuffixFilenameFilter());
            if (strings != null) {
                for (int i = 0; i < strings.length; i++) {
                    File file1 = new File(tempDir, strings[i]);
                    long bc = file1.lastModified();
                    if (ba - bc > 172800000L) {
                        file1.delete();
                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static File createTempFile() throws IOException {
        return createTempFile((String) null);
    }

    public static String getOriginalName(Object key) {
        return (String) tempFileOriginalNames.get(key);
    }

    public static String findTempFilePath(Object object) {
        Iterator iterator = tempFileOriginalNames.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            if (((String) entry.getValue()).equals(object)) {
                return (String) entry.getKey();
            }
        }

        return null;
    }

    public static boolean isZkmTempFilePath(String string) {
        return isZkmTempFile(new File(string));
    }

    public static String replaceTempPaths(String string) {
        if (tempFileOriginalNames.size() != 0 && string.indexOf(".tmp") != -1) {
            Iterator iterator = tempFileOriginalNames.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                int endIndex = string.indexOf(key);
                if (endIndex > -1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(string.substring(0, endIndex));
                    stringBuilder.append(value);
                    stringBuilder.append(string.substring(endIndex + key.length()));
                    return stringBuilder.toString();
                }
            }

            return string;
        } else {
            return string;
        }
    }

    public static boolean isZkmTempFile(File file1) {
        String name = file1.getName();
        String parent = file1.getParent();
        return name.startsWith("ZKM") && name.endsWith(".tmp") && parent != null && new File(parent).equals(new File(tempDirPath));
    }

    public static File createTempFile(String value) throws IOException {
        File file1 = File.createTempFile("ZKM", ".tmp", tempDir);
        file1.deleteOnExit();
        if (value != null) {
            tempFileOriginalNames.put(file1.getAbsolutePath(), value);
        }

        return file1;
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
        tempDirPath = System.getProperty("java.io.tmpdir", System.getProperty("user.dir"));
        tempDir = new File(tempDirPath);
        tempFileOriginalNames = ZkmUtils.createHashMap();
    }

    private TempFileManager() {
    }
}
