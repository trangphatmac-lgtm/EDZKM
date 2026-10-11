package com.zelix.klassmaster.license;

public class ObfuscatedLongDecoder {
    public static long decodeLong(String string) {
        char[] characterArray = string.toCharArray();
        char[] characterArrayValue = new char[characterArray.length];

        for (int i = 0; i < characterArray.length; i++) {
            char characterArrayValueElement = characterArray[i];
            if (characterArrayValueElement >= '0' && characterArrayValueElement <= '9') {
                if (characterArrayValueElement > '0') {
                    characterArrayValueElement = (char) (':' - characterArrayValueElement + 48);
                }
            } else if (characterArrayValueElement >= 'A' && characterArrayValueElement <= 'J') {
                characterArrayValueElement = (char) (characterArrayValueElement - 17);
            } else if (characterArrayValueElement >= 'K' && characterArrayValueElement <= 'T') {
                characterArrayValueElement = (char) (characterArrayValueElement - 27);
            } else if (characterArrayValueElement >= 'U' && characterArrayValueElement <= 'Z') {
                characterArrayValueElement = (char) (characterArrayValueElement - '%');
            } else if (characterArrayValueElement >= 'a' && characterArrayValueElement <= 'd') {
                characterArrayValueElement = (char) (characterArrayValueElement - '+');
            } else if (characterArrayValueElement >= 'e' && characterArrayValueElement <= 'n') {
                characterArrayValueElement = (char) (characterArrayValueElement - '5');
            } else if (characterArrayValueElement >= 'o' && characterArrayValueElement <= 'x') {
                characterArrayValueElement = (char) (characterArrayValueElement - '?');
            }

            characterArrayValue[i] = characterArrayValueElement;
        }

        return Long.parseLong(new String(characterArrayValue));
    }

    private ObfuscatedLongDecoder() {
    }
}
