package com.zelix.klassmaster.proguard.config.parser;

public class ProGuardConfigParseException extends Exception {
    public String eol = System.getProperty("line.separator", "\n");
    public ProGuardConfigToken currentToken;
    public int[][] expectedTokenSequences;
    public String[] tokenImage;

    public ProGuardConfigParseException(ProGuardConfigToken proGuardConfigToken, int[][] expectedTokenSequences, String[] tokenImageArgument) {
        super(initialise(proGuardConfigToken, expectedTokenSequences, tokenImageArgument));
        this.currentToken = proGuardConfigToken;
        this.expectedTokenSequences = expectedTokenSequences;
        this.tokenImage = tokenImageArgument;
    }

    private static String initialise(ProGuardConfigToken proGuardConfigToken, int[][] integerArrayArray, String[] strings) {
        String property = System.getProperty("line.separator", "\n");
        StringBuffer stringBuffer = new StringBuffer();
        int lengthValue = 0;

        for (int integerArrayArrayElementIndex = 0; integerArrayArrayElementIndex < integerArrayArray.length; integerArrayArrayElementIndex++) {
            if (lengthValue < integerArrayArray[integerArrayArrayElementIndex].length) {
                lengthValue = integerArrayArray[integerArrayArrayElementIndex].length;
            }

            for (int integerArrayArrayElementElementIndex = 0; integerArrayArrayElementElementIndex < integerArrayArray[integerArrayArrayElementIndex].length; integerArrayArrayElementElementIndex++) {
                stringBuffer.append(strings[integerArrayArray[integerArrayArrayElementIndex][integerArrayArrayElementElementIndex]]).append(' ');
            }

            if (integerArrayArray[integerArrayArrayElementIndex][integerArrayArray[integerArrayArrayElementIndex].length - 1] != 0) {
                stringBuffer.append("...");
            }

            stringBuffer.append(property).append("    ");
        }

        String string1 = "Encountered \"";
        ProGuardConfigToken proGuardConfigToken1 = proGuardConfigToken.Y;

        for (int i = 0; i < lengthValue; i++) {
            if (i != 0) {
                string1 = string1 + " ";
            }

            if (proGuardConfigToken1.s == 0) {
                string1 = string1 + strings[0];
                break;
            }

            string1 = string1 + " " + strings[proGuardConfigToken1.s];
            string1 = string1 + " \"";
            string1 = string1 + add_escapes(proGuardConfigToken1.M);
            string1 = string1 + " \"";
            proGuardConfigToken1 = proGuardConfigToken1.Y;
        }

        string1 = string1 + "\" at line " + proGuardConfigToken.Y.c + ", column " + proGuardConfigToken.Y.F;
        string1 = string1 + "." + property;
        if (integerArrayArray.length == 1) {
            string1 = string1 + "Was expecting:" + property + "    ";
        } else {
            string1 = string1 + "Was expecting one of:" + property + "    ";
        }

        return string1 + stringBuffer.toString();
    }

    public static String add_escapes(String string) {
        StringBuffer stringBuffer = new StringBuffer();

        for (int characterIndex = 0; characterIndex < string.length(); characterIndex++) {
            switch (string.charAt(characterIndex)) {
                case '\u0000':
                    break;
                case '\b':
                    stringBuffer.append("\\b");
                    break;
                case '\t':
                    stringBuffer.append("\\t");
                    break;
                case '\n':
                    stringBuffer.append("\\n");
                    break;
                case '\f':
                    stringBuffer.append("\\f");
                    break;
                case '\r':
                    stringBuffer.append("\\r");
                    break;
                case '"':
                    stringBuffer.append("\\\"");
                    break;
                case '\'':
                    stringBuffer.append("\\'");
                    break;
                case '\\':
                    stringBuffer.append("\\\\");
                    break;
                default:
                    char ba;
                    if ((ba = string.charAt(characterIndex)) >= ' ' && ba <= '~') {
                        stringBuffer.append(ba);
                    } else {
                        String string1 = "0000" + Integer.toString(ba, 16);
                        stringBuffer.append("\\u" + string1.substring(string1.length() - 4, string1.length()));
                    }
            }
        }

        return stringBuffer.toString();
    }

    public ProGuardConfigParseException() {
    }
}
