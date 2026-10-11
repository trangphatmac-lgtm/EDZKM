package com.zelix.klassmaster.archive;

import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.AssertionFailedException;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.StringKeyTransform;
import com.zelix.klassmaster.util.ZkmStringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZkmFileUtils {
    public static final String USER_DIR = System.getProperty("user.dir");
    public static final File USER_DIR_FILE = new File(USER_DIR);
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final char FILE_SEPARATOR_CHAR = FILE_SEPARATOR.charAt(0);
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");
    public static final char PATH_SEPARATOR_CHAR = PATH_SEPARATOR.charAt(0);
    public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
    public static final String DEFAULT_ENCODING = System.getProperty("file.encoding", "UTF-8");
    public static boolean caseSensitiveFileSystem = true;
    public static final String OS_NAME = System.getProperty("os.name");
    public static List extraXmlFilePatterns;

    public static List listFilesInMatchingDirectories(String string, String string1, File file1, ObservableHolder observableHolder) throws ZkmException, IOException {
        int endIndex = string.lastIndexOf(FILE_SEPARATOR);
        String string2;
        String string3;
        if (endIndex > -1) {
            string2 = string.substring(0, endIndex);
            string3 = string.substring(endIndex + 1);
        } else {
            string2 = null;
            string3 = string;
        }

        File file2;
        if (string2 == null) {
            file2 = file1;
        } else if (isRelativePath(string2)) {
            file2 = new File(file1, string2);
        } else {
            file2 = new File(string2);
        }

        Vector vector = new Vector();
        if (!file2.exists()) {
            observableHolder.setValue("Directory '" + file2.getAbsolutePath() + "' does not exist.");
            return vector;
        }

        if (!file2.isDirectory()) {
            observableHolder.setValue("Path '" + file2.getAbsolutePath() + "' is not a directory.");
            return vector;
        }

        File[] files = file2.listFiles();
        if (files == null) {
            observableHolder.setValue("Unknown error reading '" + file2.getAbsolutePath() + "'.");
            return vector;
        }

        for (File file3 : files) {
            if (file3.isDirectory() && ZkmStringUtils.matchesWildcard(file3.getName(), string3)) {
                collectMatchingFiles(file3, string1, vector);
            }
        }

        return vector;
    }

    public static byte[] readBytes(InputStream inputStream1, int ba) throws IOException {
        byte[] byteArray = null;
        if (ba > 0) {
            byteArray = new byte[ba];
            readFully(inputStream1, byteArray);
        }

        return byteArray;
    }

    
    
    public static File copyStreamToTempFile(BufferedInputStream bufferedInputStream, String value) throws IOException {
        File file1 = TempFileManager.createTempFile(value);
        BufferedOutputStream bufferedOutputStream = null;
        boolean bl = false ;

        try {
            bl = true;
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file1));
            bufferedInputStream.available();
            byte[] byteArray = new byte[2048];
            int bc = 0;
            int be = bufferedInputStream.read(byteArray);

            while (true) {
                int bb = be;
                if (be == -1) {
                    bl = false;
                    break;
                }

                bc += bb;
                int bd = bb;
                bufferedOutputStream.write(byteArray, 0, bd);
                be = bufferedInputStream.read(byteArray);
            }
        } finally {
            if (bl) {
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException iOException) {
                    }
                }
            }
        }

        try {
            bufferedOutputStream.close();
        } catch (IOException iOException1) {
        }

        return file1;
    }

    public static boolean isRelativePath(String string) {
        String string1 = string;
        if (string1 != null && string1.trim().length() != 0) {
            string1 = string1.trim();
            return (!(string1.charAt(0) == File.separatorChar) && (string1.length() <= 1 || !Character.isLetter(string1.charAt(0)) || string1.charAt(1) != ':'));
        } else {
            return false;
        }
    }

    public static boolean isNestedArchiveEntry(ZipFile zipFile1, ZipEntry zipEntry1) throws IOException {
        return looksLikeArchiveName(zipEntry1.getName()) && entryHasZipSignature(zipFile1, zipEntry1);
    }

    public static String readFileAsString(File file1) throws IOException {
        return readFileAsString(file1, DEFAULT_ENCODING);
    }

    public static BufferedReader openReader(File file1, String string) throws ZkmException, IOException {
        return createBomAwareReader(new FileInputStream(file1), string, (ObservableHolder) null);
    }

    public static byte[] peekBytes(PushbackInputStream pushbackInputStream) {
        try {
            byte[] byteArray = new byte[4];
            int bb = readFully(pushbackInputStream, byteArray);
            if (bb == -1) {
                pushbackInputStream.unread(byteArray, 0, 0);
            } else {
                int bc = bb;
                pushbackInputStream.unread(byteArray, 0, bc);
            }

            return byteArray;
        } catch (IOException iOException) {
            return null;
        }
    }

    public static void copyFile(String string, String string1) throws IOException {
        copyFile(string, string1, (Integer) null, (Integer) null);
    }

    public static String computeRelativePath(String string, String string1) {
        if (string.equals(string1)) {
            return ".";
        }

        char bb = '/';
        int characterIndex;
        if (((characterIndex = string.lastIndexOf(FILE_SEPARATOR)) > -1 || (characterIndex = string.lastIndexOf("\\")) > -1 || (characterIndex = string.lastIndexOf("/")) > -1) && characterIndex > -1) {
            bb = string.charAt(characterIndex);
        }

        if (bb == 0
                && ((characterIndex = string1.lastIndexOf(FILE_SEPARATOR)) > -1 || (characterIndex = string1.lastIndexOf("\\")) > -1 || (characterIndex = string1.lastIndexOf("/")) > -1)
                && characterIndex > -1) {
            bb = string1.charAt(characterIndex);
        }

        if (string.trim().length() == 0) {
            return string1.charAt(0) == bb ? string1.substring(1) : string1;
        }

        String prefix = String.valueOf(bb);
        StringTokenizer stringTokenizer = new StringTokenizer(string, prefix, true);
        StringTokenizer stringTokenizer1 = new StringTokenizer(string1, prefix, true);
        int bc = stringTokenizer.countTokens();
        int bd = stringTokenizer1.countTokens();
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < bc && i < bd; i++) {
            String string3 = stringTokenizer.nextToken();
            String string4 = stringTokenizer1.nextToken();
            if (!string3.equals(string4)) {
                break;
            }

            stringBuilder.append(string3);
        }

        String string6 = stringBuilder.toString();
        if (string6.length() == 0) {
            StringTokenizer stringTokenizer3 = new StringTokenizer(string, prefix);
            int bi = stringTokenizer3.countTokens();
            StringBuilder stringBuilder2 = new StringBuilder();

            for (int i = 0; i < bi; i++) {
                stringBuilder2.append("..");
                if (i < bi - 1) {
                    stringBuilder2.append(prefix);
                }
            }

            if (!string1.startsWith(prefix)) {
                stringBuilder2.append(prefix);
            }

            stringBuilder2.append(string1);
            return stringBuilder2.toString();
        } else {
            StringTokenizer stringTokenizer2 = new StringTokenizer(string6, prefix, true);
            int bh = stringTokenizer2.countTokens();
            String string5 = string1.substring(string6.length());
            StringBuilder stringBuilder1 = new StringBuilder();
            if (bc == bh) {
                stringBuilder1.append(".");
            } else {
                int bf = bc - bh;
                stringBuilder1.append("..");
                bf += -1;

                for (int i = 0; i < bf / 2; i++) {
                    stringBuilder1.append(prefix);
                    stringBuilder1.append("..");
                }
            }

            if (string5.length() > 0) {
                if (stringBuilder1.length() == 1 && stringBuilder1.toString().equals(".")) {
                    stringBuilder1.setLength(0);
                    if (string5.charAt(0) == bb) {
                        string5 = string5.substring(1);
                    }
                } else if (stringBuilder1.charAt(stringBuilder1.length() - 1) == bb) {
                    if (string5.charAt(0) == bb) {
                        stringBuilder1.setLength(stringBuilder1.length() - 1);
                    }
                } else if (string5.charAt(0) != bb) {
                    stringBuilder1.append(prefix);
                }

                stringBuilder1.append(string5);
            }

            return stringBuilder1.toString();
        }
    }

    
    
    public static File extractEntryToTempFile(ZipFile zipFile1, ZipEntry zipEntry1) throws IOException {
        BufferedInputStream bufferedInputStream = null;
        boolean bl = false ;

        File file1;
        try {
            bl = true;
            bufferedInputStream = new BufferedInputStream(zipFile1.getInputStream(zipEntry1));
            file1 = copyStreamToTempFile(bufferedInputStream, zipFile1.getName() + "!" + zipEntry1.getName());
            bl = false;
        } finally {
            if (bl) {
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException iOException) {
                    }
                }
            }
        }

        try {
            bufferedInputStream.close();
        } catch (IOException iOException1) {
        }

        return file1;
    }

    public static boolean isXmlFileName(String string) {
        int beginIndex = string.lastIndexOf(46);
        if (beginIndex > -1) {
            String string1 = string.substring(beginIndex, string.length());
            if (string1.equalsIgnoreCase(".xml") || string1.equalsIgnoreCase(".tld") || string1.equalsIgnoreCase(".fxml") || string1.equalsIgnoreCase(".e4xmi")) {
                return true;
            }
        }

        if (extraXmlFilePatterns != null) {
            Iterator iterator = extraXmlFilePatterns.iterator();

            while (iterator.hasNext()) {
                String string2 = (String) iterator.next();
                if (string2.indexOf("*") > -1 && ZkmStringUtils.matchesWildcard(string, string2)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String detectBomEncoding(byte[] byteArray) {
        return detectBomEncoding(byteArray, new MutableInt(), new MutableInt(), new MutableInt());
    }

    public static BufferedReader openReaderDetectingCharset(File file1, ObservableHolder observableHolder) throws ZkmException, IOException {
        return createBomAwareReader(new FileInputStream(file1), (String) null, observableHolder);
    }

    public static String detectBomEncoding(byte[] byteArray, MutableInt mutableInt, MutableInt mutableInt1, MutableInt mutableInt2) {
        if (byteArray == null) {
            return null;
        } else if (byteArray.length != 4) {
            throw new IllegalArgumentException("Byte Order Mark byte array length not4 : " + byteArray.length);
        } else {
            int bb = byteArray[0] & 255;
            int bc = byteArray[1] & 255;
            int bd = byteArray[2] & 255;
            if (bb == 239 && bc == 187 && bd == 191) {
                mutableInt.setValue(3);
                mutableInt1.setValue(3);
                return "UTF-8";
            } else if (bb == 255 && bc == 254) {
                mutableInt1.setValue(2);
                mutableInt2.setValue(0);
                return "UTF-16";
            } else if (bb == 254 && bc == 255) {
                mutableInt1.setValue(2);
                mutableInt2.setValue(1);
                return "UTF-16";
            } else {
                return null;
            }
        }
    }

    
    
    public static File writeStringToFile(String text, File outputFile, ObservableHolder errorMessage) throws ZkmException, IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            writer.println(text);
            writer.flush();
            return outputFile;
        } catch (IOException exception) {
            errorMessage.setValue(exception.getMessage());
            return null;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static String resolveDotRelativePath(String string, File file1) {
        File file2 = file1;
        String string1 = string;
        if (string1 == null) {
            return null;
        }

        String string2;
        if (file2 == null) {
            string2 = USER_DIR;
            file2 = USER_DIR_FILE;
        } else {
            string2 = file2.getAbsolutePath();
        }

        string1 = string1.trim();
        StringBuilder stringBuilder = new StringBuilder();
        if (string1.startsWith(".." + FILE_SEPARATOR_CHAR)) {
            File file3 = file2.getParentFile();
            if (file3 == null) {
                stringBuilder.append(string2);
            } else {
                stringBuilder.append(file3.getAbsolutePath());
            }

            if (string1.length() > 2) {
                if (stringBuilder.charAt(stringBuilder.length() - 1) == FILE_SEPARATOR_CHAR && string1.charAt(2) == FILE_SEPARATOR_CHAR) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }

                stringBuilder.append(string1.substring(2));
            }
        } else {
            if (!string1.startsWith("." + FILE_SEPARATOR_CHAR)) {
                return string1;
            }

            stringBuilder.append(string2);
            if (string1.length() > 1) {
                if (stringBuilder.charAt(stringBuilder.length() - 1) == FILE_SEPARATOR_CHAR && string1.charAt(1) == FILE_SEPARATOR_CHAR) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }

                stringBuilder.append(string1.substring(1));
            }
        }

        return stringBuilder.toString();
    }

    public static BufferedReader openReaderForPath(String string, String string1) throws ZkmException, IOException {
        return createBomAwareReader(new FileInputStream(string.trim()), string1, (ObservableHolder) null);
    }

    public static boolean isPropertiesFileName(String string) {
        if (!HiddenOptionFlags.PROCESS_PROPERTIES) {
            return false;
        }

        int beginIndex = string.lastIndexOf(46);
        return beginIndex > -1 && string.substring(beginIndex, string.length()).equalsIgnoreCase(".properties");
    }

    
    
    public static boolean fileHasZipSignature(File file1) throws IOException {
        FileInputStream fileInputStream = null;
        boolean bl1 = false ;

        boolean bl;
        try {
            bl1 = true;
            fileInputStream = new FileInputStream(file1);
            bl = streamHasZipSignature(fileInputStream);
            bl1 = false;
        } finally {
            if (bl1) {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                } catch (AssertionFailedException assertionFailedException) {
                    throw assertionFailedException;
                } catch (Exception exception) {
                }
            }
        }

        try {
            fileInputStream.close();
        } catch (AssertionFailedException assertionFailedException1) {
            throw assertionFailedException1;
        } catch (Exception exception1) {
        }

        return bl;
    }

    public static boolean entryHasZipSignature(ZipFile zipFile1, ZipEntry zipEntry1) throws IOException {
        InputStream inputStream1 = null;

        try {
            inputStream1 = zipFile1.getInputStream(zipEntry1);
            return streamHasZipSignature(inputStream1);
        } finally {
            try {
                if (inputStream1 != null) {
                    inputStream1.close();
                }
            } catch (AssertionFailedException assertionFailedException) {
                throw assertionFailedException;
            } catch (Exception exception) {
            }
        }
    }

    
    
    public static File copyFileToTemp(File file1) throws IOException {
        BufferedInputStream bufferedInputStream = null;
        boolean bl = false ;

        File file2;
        try {
            bl = true;
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file1));
            file2 = copyStreamToTempFile(bufferedInputStream, file1.getAbsolutePath());
            bl = false;
        } finally {
            if (bl) {
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException iOException) {
                    }
                }
            }
        }

        try {
            bufferedInputStream.close();
        } catch (IOException iOException1) {
        }

        return file2;
    }

    public static boolean isYamlFileName(String string) {
        if (!HiddenOptionFlags.PROCESS_YAML) {
            return false;
        }

        int beginIndex = string.lastIndexOf(46);
        if (beginIndex > -1) {
            String string1 = string.substring(beginIndex, string.length());
            if (string1.equalsIgnoreCase(".yaml") || string1.equalsIgnoreCase(".yml")) {
                return true;
            }
        }

        return false;
    }

    public static BufferedReader openReaderForPath(String string) throws ZkmException, IOException {
        return createBomAwareReader(new FileInputStream(string.trim()), (String) null, (ObservableHolder) null);
    }

    public static String readEntryAsString(ZipFile zipFile1, ZipEntry zipEntry1, String string) throws IOException {
        return readStreamAsString(zipFile1.getInputStream(zipEntry1), string, zipEntry1.getSize(), (StringKeyTransform) null);
    }

    public static boolean looksLikeArchiveName(String string) {
        int ba = string.length();
        int beginIndex = string.lastIndexOf(46);
        if (beginIndex > 0 && (beginIndex == ba - 4 || beginIndex == ba - 5)) {
            String string1 = string.substring(beginIndex).toLowerCase();
            if (string1.equals(".zip") || string1.equals(".jmod") || string1.endsWith("ar")) {
                return true;
            }
        }

        return false;
    }

    public static String splitPath(String value, ObservableHolder observableHolder, ObservableHolder observableHolder1) throws ZkmException, IOException {
        int ba;
        if ((ba = value.lastIndexOf(FILE_SEPARATOR)) <= -1 && (ba = value.lastIndexOf("\\")) <= -1 && (ba = value.lastIndexOf("/")) <= -1) {
            observableHolder.setValue("");
            observableHolder1.setValue(value);
            return "";
        } else {
            observableHolder.setValue(value.substring(0, ba));
            observableHolder1.setValue(value.substring(ba + 1));
            return value.substring(ba, ba + 1);
        }
    }

    
    
    public static boolean canWriteAndReadFile(String fileName, ObservableHolder errorMessage) throws ZkmException, IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(fileName));
            writer.println("xXyYzZ");
        } catch (IOException exception) {
            if (errorMessage != null) {
                errorMessage.setValue(exception.toString());
            }
            return false;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        BufferedReader reader = null;
        String firstLine;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            firstLine = reader.readLine();
        } catch (IOException exception) {
            if (errorMessage != null) {
                errorMessage.setValue(exception.toString());
            }
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException exception) {
                }
            }
        }
        return firstLine != null && "xXyYzZ".equals(firstLine);
    }

    public static int readFully(InputStream inputStream1, byte[] byteArray) throws IOException {
        int bb = 0;

        do {
            int bc = inputStream1.read(byteArray, bb, byteArray.length - bb);
            if (bc == -1) {
                if (bb == 0) {
                    bb = -1;
                }
                break;
            }

            bb += bc;
        } while (bb < byteArray.length);

        return bb;
    }

    public static String getParentPath(String string) {
        String string1 = string;
        String string2;
        String fileSeparator;
        if (string1.length() > 1) {
            char ba = string1.charAt(string1.length() - 1);
            if (ba != FILE_SEPARATOR.charAt(0) && ba != "/".charAt(0) && ba != "\\".charAt(0)) {
                string2 = string1;
                fileSeparator = FILE_SEPARATOR;
            } else {
                string1 = string1.substring(0, string1.length() - 1);
                string2 = string1;
                fileSeparator = FILE_SEPARATOR;
            }
        } else {
            string2 = string1;
            fileSeparator = FILE_SEPARATOR;
        }

        int endIndex;
        if ((endIndex = string2.lastIndexOf(fileSeparator)) <= -1 && (endIndex = string1.lastIndexOf("/")) <= -1 && (endIndex = string1.lastIndexOf("\\")) <= -1) {
            return string1.length() > 0 ? "" : null;
        } else if (endIndex == 0) {
            return string1.length() == 1 ? null : string1.substring(0, endIndex + 1);
        } else {
            return string1.substring(0, endIndex);
        }
    }

    public static BufferedReader createBomAwareReader(InputStream inputStream1, String string, ObservableHolder observableHolder) throws ZkmException, IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream1, 4);
        String value = null;
        if (string != null && string.trim().length() > 0) {
            value = string.trim();
        }

        boolean bl = false;
        if (value == null || value.length() == 0 || value.equals("UTF-8")) {
            String valueValue = detectBomEncoding(peekBytes(pushbackInputStream));
            bl = valueValue != null && valueValue.equals("UTF-8");
            if (valueValue != null && valueValue.length() > 0 && (value == null || value.length() == 0)) {
                value = valueValue;
            }
        }

        BufferedReader bufferedReader;
        if (value != null && value.length() != 0) {
            if (bl) {
                readBytes(pushbackInputStream, 3);
            }

            bufferedReader = new BufferedReader(new InputStreamReader(pushbackInputStream, value));
        } else {
            bufferedReader = new BufferedReader(new InputStreamReader(pushbackInputStream));
        }

        if (observableHolder != null) {
            observableHolder.setValue(value);
        }

        return bufferedReader;
    }

    public static BufferedReader openReader(File file1) throws ZkmException, IOException {
        return createBomAwareReader(new FileInputStream(file1), (String) null, (ObservableHolder) null);
    }

    public static String describeEntryWithOriginal(ZipFile zipFile1, ZipEntry zipEntry1) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(zipFile1.getName());
        File file1 = new File(zipFile1.getName());
        if (TempFileManager.isZkmTempFile(file1)) {
            String originalName = TempFileManager.getOriginalName(file1.getAbsolutePath());
            if (originalName != null) {
                stringBuilder.append('(');
                stringBuilder.append(originalName);
                stringBuilder.append(')');
            }
        }

        stringBuilder.append("!");
        stringBuilder.append(zipEntry1.getName());
        return stringBuilder.toString();
    }

    public static String getEntryPath(ZipFile zipFile1, ZipEntry zipEntry1) {
        return zipFile1.getName() + "!" + zipEntry1.getName();
    }

    public static boolean isKnownArchiveName(String string) {
        int ba = string.length();
        if (ba > 4) {
            String string1 = string.substring(ba - 4);
            if (string1.equalsIgnoreCase(".zip")
                    || string1.equalsIgnoreCase(".jar")
                    || string1.equalsIgnoreCase(".ear")
                    || string1.equalsIgnoreCase(".war")
                    || string1.equalsIgnoreCase(".rar")
                    || string1.equalsIgnoreCase(".sar")
                    || string1.equalsIgnoreCase(".par")
                    || string1.equalsIgnoreCase(".aar")) {
                return true;
            }

            if (ba > 5 && string.substring(ba - 5).equalsIgnoreCase(".jmod")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isArchiveFile(File file1) throws IOException {
        String nameValue = file1.getName();
        return isKnownArchiveName(nameValue) || looksLikeArchiveName(nameValue) && fileHasZipSignature(file1);
    }

    public static String readFileTransformed(File file1, String string, StringKeyTransform stringKeyTransform) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file1);
        return readStreamAsString(fileInputStream, string, fileInputStream.available() + 10, stringKeyTransform);
    }

    public static File writeStringToTempFile(String textArgument, ObservableHolder observableHolder) throws ZkmException, IOException {
        try {
            return writeStringToFile(textArgument, TempFileManager.createTempFile(), observableHolder);
        } catch (IOException iOException) {
            observableHolder.setValue(iOException.getMessage());
            return null;
        }
    }

    public static void collectMatchingFiles(File file1, String string, List list1) {
        for (File file2 : file1.listFiles()) {
            if (file2.isDirectory()) {
                collectMatchingFiles(file2, string, list1);
            } else {
                String nameValue = file2.getName();
                if (!caseSensitiveFileSystem) {
                    nameValue = nameValue.toUpperCase();
                }

                if (ZkmStringUtils.matchesWildcard(nameValue, string)) {
                    list1.add(file2.getAbsolutePath());
                }
            }
        }
    }

    public static String normalizeSeparators(String string) {
        String string1 = string;
        string1 = string1.trim();
        String string2;
        char ba;
        if (string1.length() > 1) {
            if (Character.isLetter(string1.charAt(0))) {
                if (string1.charAt(1) == ':') {
                    if (File.separatorChar == '\\') {
                        return string1.replace('/', File.separatorChar);
                    }

                    return string1;
                }

                string2 = string1;
                ba = '\\';
            } else {
                string2 = string1;
                ba = '\\';
            }
        } else {
            string2 = string1;
            ba = '\\';
        }

        string1 = string2.replace(ba, File.separatorChar);
        return string1.replace('/', File.separatorChar);
    }

    public static String readFileAsString(File file1, String string) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file1);
        return readStreamAsString(fileInputStream, string, fileInputStream.available() + 10, (StringKeyTransform) null);
    }

    public static String urlDecode(String string) {
        String string1 = null;

        try {
            string1 = URLDecoder.decode(string, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException unsupportedEncodingException1) {
            try {
                string1 = URLDecoder.decode(string, "UTF-8");
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
            }
        }

        return string1 != null ? string1 : string;
    }

    
    
    public static String readStreamAsString(InputStream inputStream1, String string, long ba, StringKeyTransform stringKeyTransform) throws IOException {
        BufferedReader bufferedReader = null;
        StringWriter stringWriter;
        if (ba > -1L && ba < 2147483647L) {
            stringWriter = new StringWriter((int) ba + 10);
        } else {
            stringWriter = new StringWriter();
        }

        boolean bl1 = false ;

        try {
            bl1 = true;
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream1, string));
            boolean bl = true;

            String string1;
            while ((string1 = bufferedReader.readLine()) != null) {
                if (stringKeyTransform != null) {
                    string1 = stringKeyTransform.transformLine(string1);
                }

                if (bl) {
                    bl = false;
                    stringWriter.write(string1);
                } else {
                    stringWriter.append(LINE_SEPARATOR);
                    stringWriter.write(string1);
                }
            }

            bl1 = false;
        } finally {
            if (bl1) {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException iOException) {
                    }
                } else {
                    try {
                        inputStream1.close();
                    } catch (Exception exception) {
                    }
                }
            }
        }

        try {
            bufferedReader.close();
        } catch (IOException iOException1) {
            return stringWriter.toString();
        }

        return stringWriter.toString();
    }

    public static boolean containsWildcard(String string) {
        return string.indexOf("*") > -1;
    }

    public static boolean streamHasZipSignature(InputStream inputStream1) throws IOException {
        if (inputStream1.available() >= 4) {
            byte[] byteArray = readBytes(inputStream1, 4);
            return (byteArray[0] & 255) == 80 && (byteArray[1] & 255) == 75 && (byteArray[2] & 255) == 3 && (byteArray[3] & 255) == 4;
        } else {
            return false;
        }
    }

    static {
        String string = "aAzZyY".toUpperCase();
        int ba = 0;

        File file1;
        File file2;
        do {
            file1 = new File("aAzZyY" + ba + ".txt");
            String string2 = string + ba++ + ".txt";
            file2 = new File(string2);
        } while (file1.exists() || file2.exists());

        PrintWriter printWriter = null;

        try {
            printWriter = new PrintWriter(new FileWriter(file1), true);
            printWriter.println("aAzZyY");
            printWriter.flush();
            caseSensitiveFileSystem = !file2.exists();
        } catch (IOException exception) {
            caseSensitiveFileSystem = !OS_NAME.startsWith("Windows") && !OS_NAME.startsWith("OS/2") && !OS_NAME.startsWith("Mac OS");
        } finally {
            printWriter.close();
        }
        file1.delete();

        if (HiddenOptionFlags.EXTRA_XML_FILE_TYPES != null) {
            extraXmlFilePatterns = new ArrayList();
            StringTokenizer stringTokenizer = new StringTokenizer(HiddenOptionFlags.EXTRA_XML_FILE_TYPES, ";");

            while (stringTokenizer.hasMoreTokens()) {
                String string1 = stringTokenizer.nextToken();
                List list1;
                if (!caseSensitiveFileSystem) {
                    string1 = string1.toLowerCase();
                    list1 = extraXmlFilePatterns;
                } else {
                    list1 = extraXmlFilePatterns;
                }

                list1.add(string1);
            }
        }
    }

    public static String getLastPathComponent(String string) {
        if (string == null) {
            return null;
        }

        String lastPathComponent = string.trim();
        if (FILE_SEPARATOR_CHAR == '\\') {
            lastPathComponent = lastPathComponent.replace('/', FILE_SEPARATOR_CHAR);
        } else if (FILE_SEPARATOR_CHAR == '/') {
            lastPathComponent = lastPathComponent.replace('\\', FILE_SEPARATOR_CHAR);
        }

        while (lastPathComponent.endsWith(FILE_SEPARATOR)) {
            lastPathComponent = lastPathComponent.substring(0, lastPathComponent.length() - FILE_SEPARATOR.length());
        }

        int ba = lastPathComponent.lastIndexOf(FILE_SEPARATOR_CHAR);
        if (ba > -1) {
            return ba == lastPathComponent.length() - 1 ? lastPathComponent.substring(0, lastPathComponent.length() - 1) : lastPathComponent.substring(ba + 1);
        } else {
            return lastPathComponent;
        }
    }

    public static List expandFilePath(String string, File file1, ObservableHolder observableHolder) throws ZkmException, IOException {
        observableHolder.clearValue();
        Vector vector = new Vector();
        ObservableHolder observableHolder1;
        String value;
        if (string != null) {
            if (string.trim().length() != 0) {
                String string1 = string.trim();
                string1 = normalizeSeparators(string1);
                string1 = resolveDotRelativePath(string1, file1);
                if (containsWildcard(string1)) {
                    int endIndex = string1.lastIndexOf(FILE_SEPARATOR);
                    File file2;
                    String string2;
                    if (endIndex > 0) {
                        string2 = string1.substring(endIndex + 1, string1.length());
                        String string3 = string1.substring(0, endIndex);
                        if (containsWildcard(string3)) {
                            int bb = string3.indexOf("*");
                            int bc = string3.lastIndexOf(FILE_SEPARATOR);
                            if (bb < bc) {
                                observableHolder.setValue("Wildcard not in final directory qualifier : '" + string3 + "'");
                                return vector;
                            }

                            if (!caseSensitiveFileSystem) {
                                string2 = string2.toUpperCase();
                            }

                            return listFilesInMatchingDirectories(string3, string2, file1, observableHolder);
                        }

                        if (isRelativePath(string3)) {
                            file2 = new File(file1, string3);
                        } else {
                            file2 = new File(string3);
                        }
                    } else {
                        file2 = file1;
                        string2 = string1;
                    }

                    if (!caseSensitiveFileSystem) {
                        string2 = string2.toUpperCase();
                    }

                    if (file2.isDirectory() && file2.exists()) {
                        File[] files = file2.listFiles();
                        if (files != null) {
                            for (int fileIndex = 0; fileIndex < files.length; fileIndex++) {
                                String nameValue = files[fileIndex].getName();
                                if (!caseSensitiveFileSystem) {
                                    nameValue = nameValue.toUpperCase();
                                }

                                if (ZkmStringUtils.matchesWildcard(nameValue, string2)) {
                                    vector.add(files[fileIndex].getAbsolutePath());
                                }
                            }
                        } else {
                            observableHolder.setValue("' File error trying to access " + file2.getAbsolutePath() + "' : '" + string1 + "'");
                        }
                    } else {
                        observableHolder.setValue("'" + file2.getAbsolutePath() + "' is not a directory or does not exist : '" + string1 + "'");
                    }
                } else {
                    File file3 = new File(string1);
                    vector.add(file3.getAbsolutePath());
                }

                return vector;
            }

            observableHolder1 = observableHolder;
            value = "No file path specified";
        } else {
            observableHolder1 = observableHolder;
            value = "No file path specified";
        }

        observableHolder1.setValue(value);
        return vector;
    }

    public static List getExtraXmlFilePatterns() {
        return extraXmlFilePatterns;
    }

    public static String resolveAgainstBase(String string, String string1) {
        String string2 = string;
        if (string2 == null) {
            return null;
        }

        if (string1 == null) {
            throw new IllegalArgumentException("Must specify a base directory. Cannot translate '" + string2 + "'.");
        }

        string2 = string2.trim();
        StringBuilder stringBuilder = new StringBuilder();
        if (string2.startsWith("..")) {
            String string3 = string2;
            String parentPathValue = string1;
            String string7 = string3;

            for (String prefix = ".."; string7.startsWith(prefix); prefix = "..") {
                String parentPath = getParentPath(parentPathValue);
                if (parentPath == null) {
                    throw new IllegalArgumentException("Base directory '" + string1 + "' has inadequate parent. Cannot translate '" + string2 + "'.");
                }

                parentPathValue = parentPath;
                if (string3.length() > 2) {
                    string3 = string3.substring(3);
                } else {
                    string3 = "";
                }

                string7 = string3;
            }

            stringBuilder.append(parentPathValue);
            if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) != '/' && string3.length() > 0) {
                stringBuilder.append('/');
            }

            stringBuilder.append(string3);
        } else if (string2.startsWith(".")) {
            if (string1.length() > 0) {
                stringBuilder.append(string1);
                if (string2.length() > 1) {
                    if (stringBuilder.charAt(stringBuilder.length() - 1) == '/' && string2.charAt(1) == '/') {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }

                    stringBuilder.append(string2.substring(1));
                }
            } else {
                String string6 = string2.substring(1);
                if (string6.length() > 0 && string6.charAt(0) == '/') {
                    string6 = string6.substring(1);
                    stringBuilder.append(string6);
                }
            }
        } else {
            if (string2.charAt(0) == '/') {
                return string2;
            }

            if (string1.length() > 0) {
                stringBuilder.append(string1);
                if (stringBuilder.charAt(stringBuilder.length() - 1) != '/') {
                    stringBuilder.append('/');
                }
            }

            stringBuilder.append(string2);
        }

        return stringBuilder.toString();
    }

    public static BufferedReader createBomAwareReader(InputStream inputStream1) throws ZkmException, IOException {
        return createBomAwareReader(inputStream1, (String) null, (ObservableHolder) null);
    }

    
    
    public static void copyFile(String string, String string1, Integer integer, Integer integer1) throws IOException {
        if (new File(string).getAbsolutePath().equals(new File(string1).getAbsolutePath())) {
            throw new IllegalArgumentException("Source and destination files are the same : '" + new File(string).getAbsolutePath() + "'");
        }

        BufferedOutputStream bufferedOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        boolean bl = false ;

        try {
            bl = true;
            if (integer != null) {
                bufferedInputStream = new BufferedInputStream(new FileInputStream(string), integer);
            } else {
                bufferedInputStream = new BufferedInputStream(new FileInputStream(string));
            }

            int bf;
            if (integer1 != null) {
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(string1), integer1);
                bf = bufferedInputStream.available();
            } else {
                bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(string1));
                bf = bufferedInputStream.available();
            }

            int ba = bf;
            byte[] byteArray = new byte[ba];
            int bc = 0;

            while (bc < ba) {
                int be = ba - bc;
                int bd;
                if ((bd = bufferedInputStream.read(byteArray, bc, Math.min(8192, be))) == -1) {
                    break;
                }

                bc += bd;
            }

            bufferedOutputStream.write(byteArray);
            bl = false;
        } finally {
            if (bl) {
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException iOException1) {
                    }
                }

                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException iOException) {
                    }
                }
            }
        }

        try {
            bufferedOutputStream.close();
        } catch (IOException iOException3) {
        }

        try {
            bufferedInputStream.close();
        } catch (IOException iOException2) {
        }
    }

    private ZkmFileUtils() {
    }
}
