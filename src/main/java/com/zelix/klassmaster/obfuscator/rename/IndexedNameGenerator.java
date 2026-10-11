package com.zelix.klassmaster.obfuscator.rename;

import java.util.ArrayList;

public class IndexedNameGenerator extends JavaKeywords {
    private ArrayList nameCache = new ArrayList(1024);
    public char[] charBuffer = new char[6];
    public int caseMode = 0;
    public String prefix;
    public final int firstCharRadix;
    public final int subsequentCharRadix;

    public final char getSubsequentChar(int ba) {
        char subsequentChar;
        if (this.caseMode == 0) {
            if (ba <= 25) {
                subsequentChar = (char) (97 + ba);
            } else if (ba <= 51) {
                subsequentChar = (char) (39 + ba);
            } else {
                subsequentChar = (char) (ba - 4);
            }
        } else if (ba <= 25) {
            subsequentChar = (char) (97 + ba);
        } else {
            subsequentChar = (char) (22 + ba);
        }

        return subsequentChar;
    }

    public final String getNonKeywordName(int elementIndex) {
        String nameAt;
        do {
            nameAt = this.getNameAt(elementIndex);
        } while (nameAt == null);

        return nameAt;
    }

    public final String getNameAt(int elementIndex) {
        if (elementIndex < this.nameCache.size()) {
            return (String) this.nameCache.get(elementIndex);
        }

        int bb = 0;
        this.charBuffer[bb++] = this.getFirstChar(elementIndex % this.firstCharRadix);

        for (int i = elementIndex / this.firstCharRadix; i > 0; i /= this.subsequentCharRadix) {
            this.charBuffer[bb++] = this.getSubsequentChar(i % this.subsequentCharRadix);
        }

        String nameAt = new String(this.charBuffer, 0, bb);
        if (this.prefix != null) {
            nameAt = this.prefix + nameAt;
        }

        if (JavaKeywords.RESERVED_WORDS.contains(nameAt)) {
            nameAt = null;
        }

        if (elementIndex == this.nameCache.size()) {
            this.nameCache.add(nameAt);
        }

        return nameAt;
    }

    public IndexedNameGenerator() {
        this(null);
    }

    public IndexedNameGenerator(String prefixArgument) {
        this.prefix = prefixArgument;
        this.firstCharRadix = 52;
        this.subsequentCharRadix = 62;
    }

    public final char getFirstChar(int ba) {
        return ba <= 25 ? (char) (97 + ba) : (char) (39 + ba);
    }
}
