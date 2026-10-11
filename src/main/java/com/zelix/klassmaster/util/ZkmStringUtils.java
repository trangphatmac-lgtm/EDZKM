package com.zelix.klassmaster.util;

import java.io.StringWriter;
import java.util.StringTokenizer;

public class ZkmStringUtils {
    public static String replaceAll(String string, String searchText, String string2) {
        if (string == null) {
            throw new IllegalArgumentException("Null String");
        }

        int ba = string.length();
        StringWriter stringWriter = new StringWriter(ba);
        char[] characterArray = string.toCharArray();
        int searchTextLength = searchText.length();
        int fromIndex = 0;

        int be;
        while (fromIndex < ba && (be = string.indexOf(searchText, fromIndex)) > -1) {
            stringWriter.write(characterArray, fromIndex, be - fromIndex);
            stringWriter.write(string2);
            fromIndex = be + searchTextLength;
        }

        stringWriter.write(characterArray, fromIndex, ba - fromIndex);
        return stringWriter.toString();
    }

    public static boolean matchesWildcard(String string, String string1) {
        if (!string1.equals("*") && !string.equals(string1)) {
            if (string.indexOf("*") == -1 && string1.indexOf("*") == -1) {
                return string.equals(string1);
            }

            StringTokenizer stringTokenizer = new StringTokenizer(string1, "*", true);
            String[] strings = new String[stringTokenizer.countTokens()];
            int ba = 0;

            while (stringTokenizer.hasMoreTokens()) {
                strings[ba++] = stringTokenizer.nextToken();
            }

            return matchWildcardTokens(string, 0, strings, 0);
        } else {
            return true;
        }
    }

    public static String repeatChar(int ba, Integer integer) {
        StringBuilder stringBuilder = new StringBuilder(ba);

        for (int i = 0; i < ba; i++) {
            stringBuilder.append((char) integer.intValue());
        }

        return stringBuilder.toString();
    }

    private static boolean matchWildcardTokens(String string, int fromIndex, String[] strings, int bb) {
        if (fromIndex >= string.length()) {
            if (bb >= strings.length) {
                return true;
            }

            while (bb < strings.length) {
                if (!strings[bb].equals("*")) {
                    return false;
                }

                bb++;
            }

            return true;
        } else {
            if (bb >= strings.length) {
                return false;
            }

            String searchText = strings[bb];
            if (!searchText.equals("*")) {
                boolean bl = string.regionMatches(fromIndex, searchText, 0, searchText.length());
                if (!bl) {
                    return false;
                }

                fromIndex += searchText.length();
                return matchWildcardTokens(string, fromIndex, strings, bb + 1);
            } else {
                while (searchText.equals("*")) {
                    if (++bb >= strings.length) {
                        break;
                    }

                    searchText = strings[bb];
                }

                if (searchText.equals("*")) {
                    return true;
                }

                while (fromIndex < string.length()) {
                    int fromIndexValue = string.indexOf(searchText, fromIndex);
                    if (fromIndexValue == -1) {
                        return false;
                    }

                    fromIndex = fromIndexValue;
                    if (matchWildcardTokens(string, fromIndex + searchText.length(), strings, bb + 1)) {
                        return true;
                    }

                    fromIndex++;
                }

                return false;
            }
        }
    }

    public static int countOccurrences(String string, String searchText) {
        if (string != null && string.length() > 0 && searchText != null && searchText.length() > 0) {
            int searchTextLength = searchText.length();
            int bb = -1;
            int fromIndex = 0 - searchTextLength;

            do {
                fromIndex += searchTextLength;
                fromIndex = string.indexOf(searchText, fromIndex);
                bb++;
            } while (fromIndex != -1);

            return bb;
        } else {
            return 0;
        }
    }

    public static int skipWhitespace(String string, int ba) {
        int bb = string.length();
        if (bb == 0) {
            return -1;
        }

        int characterIndex = ba;
        char bd = string.charAt(characterIndex);
        boolean whitespace = Character.isWhitespace(bd);

        while (whitespace) {
            if (++characterIndex == bb) {
                return -1;
            }

            bd = string.charAt(characterIndex);
            whitespace = Character.isWhitespace(bd);
        }

        return characterIndex;
    }

    public static String escapeJavaString(String string) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int characterIndex = 0; characterIndex < string.length(); characterIndex++) {
            char ba = string.charAt(characterIndex);
            switch (ba) {
                case '\u0000':
                    break;
                case '\b':
                    stringBuilder.append("\\b");
                    break;
                case '\t':
                    stringBuilder.append("\\t");
                    break;
                case '\n':
                    stringBuilder.append("\\n");
                    break;
                case '\f':
                    stringBuilder.append("\\f");
                    break;
                case '\r':
                    stringBuilder.append("\\r");
                    break;
                case '"':
                    stringBuilder.append("\\\"");
                    break;
                case '\'':
                    stringBuilder.append("\\'");
                    break;
                case '\\':
                    stringBuilder.append("\\\\");
                    break;
                default:
                    if (ba >= ' ' && ba <= '~') {
                        stringBuilder.append(ba);
                    } else {
                        stringBuilder.append("\\u" + pad(Integer.toHexString(ba), 82, 4, 48));
                    }
            }
        }

        return stringBuilder.toString();
    }

    public static int indexOfAny(String string, int ba, char[] characterArray) {
        int bc = string.length();

        for (int characterIndex = ba; characterIndex < bc; characterIndex++) {
            char be = string.charAt(characterIndex);

            for (int characterArrayElementIndex = 0; characterArrayElementIndex < characterArray.length; characterArrayElementIndex++) {
                if (be == characterArray[characterArrayElementIndex]) {
                    return characterIndex;
                }
            }
        }

        return -1;
    }

    public static String pad(String string, int ba, int endIndex, int bc) {
        int bd = string.length();
        StringBuilder stringBuilder;
        switch (ba) {
            case 76:
                stringBuilder = new StringBuilder(string);
                int bg = endIndex - bd;

                for (int i = 0; i < bg; i++) {
                    stringBuilder.append((char) bc);
                }
                break;
            case 82:
                stringBuilder = new StringBuilder();
                int be = endIndex - bd;

                for (int i = 0; i < be; i++) {
                    stringBuilder.append((char) bc);
                }

                stringBuilder.append(string);
                break;
            default:
                throw new IllegalArgumentException("Invalid justification");
        }

        return stringBuilder.toString().substring(0, endIndex);
    }

    public static int countChar(String string, char ba) {
        if (string != null && string.length() > 0) {
            char[] characterArray = string.toCharArray();
            int bc = 0;
            char[] characterArrayValue = characterArray;
            int lengthValue = characterArrayValue.length;

            for (int characterArrayValueElementIndex = 0; characterArrayValueElementIndex < lengthValue; characterArrayValueElementIndex++) {
                if (characterArrayValue[characterArrayValueElementIndex] == ba) {
                    bc++;
                }
            }

            return bc;
        } else {
            return 0;
        }
    }

    private ZkmStringUtils() {
    }
}
