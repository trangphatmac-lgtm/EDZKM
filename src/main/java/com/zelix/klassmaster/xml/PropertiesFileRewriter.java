package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.archive.ZipEntryWriter;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PropertiesFileRewriter {
    private static String markerString;
    private static final String TOKEN_DELIMITERS;

    public static void rewriteClassNames(final InputStream inputStream1Argument, final OutputStream outputStream, final String s, final int n, final EnumerableMap enumerableMap, final ObservableHolder observableHolder) throws ZkmException, IOException {
        ZkmFileUtils.readBytes(inputStream1Argument, n);
        BufferedReader bufferedReader = null;
        ByteArrayOutputStream out = null;
        PrintWriter printWriter = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream1Argument, s));
            out = new ByteArrayOutputStream();
            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, s)));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                final int skipWhitespace = ZkmStringUtils.skipWhitespace(line, 0);
                final StringBuilder stringBuilder = new StringBuilder(line.length());
                String substring;
                if (skipWhitespace > 0) {
                    stringBuilder.append(line.substring(0, skipWhitespace));
                    substring = line.substring(skipWhitespace);
                } else {
                    substring = line;
                }
                if (substring.isEmpty()) {
                    continue;
                }
                if (substring.charAt(0) == '#' || substring.charAt(0) == '!') {
                    stringBuilder.append(substring);
                } else {
                    boolean b = false;
                    final StringTokenizer stringTokenizer = new StringTokenizer(substring, PropertiesFileRewriter.TOKEN_DELIMITERS, true);
                    while (stringTokenizer.hasMoreTokens()) {
                        final String nextToken = stringTokenizer.nextToken();
                        if (nextToken.length() == 1) {
                            if (nextToken.equals(":") || nextToken.equals("=")) {
                                b = true;
                            }
                            stringBuilder.append(nextToken);
                        } else if (b) {
                            final String dotsToSlashes = ZkmUtils.dotsToSlashes(nextToken);
                            final String anObject = (String) ZkmUtils.mapOrSelf(dotsToSlashes, enumerableMap);
                            if (!dotsToSlashes.equals(anObject)) {
                                stringBuilder.append(ZkmUtils.slashesToDots(anObject));
                            } else {
                                stringBuilder.append(nextToken);
                            }
                        } else {
                            stringBuilder.append(nextToken);
                        }
                    }
                }
                printWriter.println(stringBuilder.toString());
            }
            final byte[] byteArray = out.toByteArray();
            observableHolder.setValue(byteArray);
            outputStream.write(byteArray);
            outputStream.flush();
            try {
                bufferedReader.close();
            } catch (final IOException ioException) {
            }
            printWriter.close();
            try {
                out.close();
            } catch (final IOException ioExceptionValue) {
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException ioExceptionValue2) {
                }
            }
            if (printWriter != null) {
                printWriter.close();
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (final IOException ioExceptionValue3) {
            }
        }
    }

    public PropertiesFileRewriter(
            ZipFile zipFile1,
            ZipEntry zipEntry1,
            Long long1,
            MutableInt mutableInt,
            MutableInt mutableInt1,
            MutableInt mutableInt2,
            String string,
            ZipOutputStream zipOutputStream1,
            ResourcePathTranslator resourcePathTranslator1,
            boolean compressed,
            EnumerableMap enumerableMap,
            ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        InputStream inputStream1 = null;

        try {
            inputStream1 = zipFile1.getInputStream(zipEntry1);
            String entryName = resourcePathTranslator1.translateResourcePath(zipEntry1.getName());
            ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream1, entryName, compressed);
            OutputStream outputStream = zipEntryWriter.getOutputStream();
            int value = mutableInt.getValue();
            mutableInt1.getValue();
            mutableInt2.getValue();
            zipEntry1.getName();
            EnumerableMap enumerableMap1 = enumerableMap;
            rewriteClassNames(inputStream1, outputStream, string, value, enumerableMap1, observableHolder);
            zipEntryWriter.writeEntry(long1);
        } finally {
            try {
                if (inputStream1 != null) {
                    inputStream1.close();
                }
            } catch (IOException iOException) {
            }
        }
    }

    public static String getMarkerString() {
        return markerString;
    }

    public PropertiesFileRewriter(final File file, final MutableInt mutableInt, final MutableInt mutableInt2, final MutableInt mutableInt3, final String s, final ZipOutputStream zipOutputStream, final boolean compressed, final EnumerableMap enumerableMap, final ObservableHolder observableHolder) throws ZkmException, IOException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            final ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, file.getName(), compressed);
            final FileInputStream fileInputStream2 = fileInputStream;
            final OutputStream outputStream = zipEntryWriter.getOutputStream();
            final int value = mutableInt.getValue();
            mutableInt2.getValue();
            mutableInt3.getValue();
            file.getAbsolutePath();
            rewriteClassNames(fileInputStream2, outputStream, s, value, enumerableMap, observableHolder);
            zipEntryWriter.writeEntry();
            try {
                fileInputStream.close();
            } catch (final IOException ioException) {
            }
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (final IOException ioExceptionValue) {
            }
        }
    }

    public PropertiesFileRewriter(final File file, final MutableInt mutableInt, final MutableInt mutableInt2, final MutableInt mutableInt3, final String s, final File file2, final EnumerableMap enumerableMap) throws ZkmException, IOException {
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            final File parentFile = file2.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            fileInputStream = new FileInputStream(file);
            fileOutputStream = new FileOutputStream(file2);
            final FileInputStream fileInputStream2 = fileInputStream;
            final FileOutputStream fileOutputStream2 = fileOutputStream;
            final int value = mutableInt.getValue();
            mutableInt2.getValue();
            mutableInt3.getValue();
            file.getAbsolutePath();
            rewriteClassNames(fileInputStream2, fileOutputStream2, s, value, enumerableMap, new ObservableHolder());
            try {
                fileInputStream.close();
            } catch (final IOException ioException) {
            }
            try {
                fileOutputStream.close();
            } catch (final IOException ioExceptionValue) {
            }
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (final IOException ioExceptionValue2) {
            }
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (final IOException ioExceptionValue3) {
            }
        }
    }

    public static void setMarkerString() {
        markerString = "MBppWc";
    }

    static {
        if (getMarkerString() == null) {
            setMarkerString();
        }

        TOKEN_DELIMITERS = " \t=:\\!#";
    }
}
