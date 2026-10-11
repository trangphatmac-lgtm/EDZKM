package com.zelix.klassmaster.changelog.parser;

import java.io.IOException;
import java.io.Reader;

public class ChangeLogSimpleCharStream {
    public static int bufsize;
    public static int[] bufline;
    public static int available;
    public static int tokenBegin;
    public static int[] bufcolumn;
    public static Reader inputStream;
    public static char[] buffer;
    private static final String SECOND_CONSTRUCTOR_ERROR = "\n   ERROR: Second call to the constructor of a static SimpleCharStream.\n       You must either use ReInit() or set the JavaCC option STATIC to false\n       during the generation of this class.";
    public static int bufpos = -1;
    public static int column = 0;
    public static int line = 1;
    public static boolean prevCharIsCR = false;
    public static boolean prevCharIsLF = false;
    public static int maxNextCharInd = 0;
    public static int inBuf = 0;
    public static int tabSize = 8;

    public static int getColumn() {
        return bufcolumn[bufpos];
    }

    public static void backup(int ba) {
        inBuf += ba;
        if ((bufpos -= ba) < 0) {
            bufpos = bufpos + bufsize;
        }
    }

    public static String GetImage() {
        return bufpos >= tokenBegin
                ? new String(buffer, tokenBegin, bufpos - tokenBegin + 1)
                : new String(buffer, tokenBegin, bufsize - tokenBegin) + new String(buffer, 0, bufpos + 1);
    }

    public void ReInit(Reader reader1) {
        inputStream = reader1;
        line = 1;
        column = 1 - 1;
        if (buffer == null || 4096 != buffer.length) {
            bufsize = 4096;
            available = 4096;
            buffer = new char[4096];
            bufline = new int[4096];
            bufcolumn = new int[4096];
        }

        prevCharIsCR = false;
        prevCharIsLF = false;
        maxNextCharInd = 0;
        inBuf = 0;
        tokenBegin = 0;
        bufpos = -1;
    }

    public static char[] GetSuffix(int copyLength) {
        char[] destinationArray = new char[copyLength];
        if (bufpos + 1 >= copyLength) {
            System.arraycopy(buffer, bufpos - copyLength + 1, destinationArray, 0, copyLength);
        } else {
            System.arraycopy(buffer, bufsize - (copyLength - bufpos - 1), destinationArray, 0, copyLength - bufpos - 1);
            System.arraycopy(buffer, 0, destinationArray, copyLength - bufpos - 1, bufpos + 1);
        }

        return destinationArray;
    }

    public static void ExpandBuff(boolean bl) {
        char[] bufferValue = new char[bufsize + 2048];
        int[] buflineValue = new int[bufsize + 2048];
        int[] bufcolumnValue = new int[bufsize + 2048];

        try {
            if (bl) {
                System.arraycopy(buffer, tokenBegin, bufferValue, 0, bufsize - tokenBegin);
                System.arraycopy(buffer, 0, bufferValue, bufsize - tokenBegin, bufpos);
                buffer = bufferValue;
                System.arraycopy(bufline, tokenBegin, buflineValue, 0, bufsize - tokenBegin);
                System.arraycopy(bufline, 0, buflineValue, bufsize - tokenBegin, bufpos);
                bufline = buflineValue;
                System.arraycopy(bufcolumn, tokenBegin, bufcolumnValue, 0, bufsize - tokenBegin);
                System.arraycopy(bufcolumn, 0, bufcolumnValue, bufsize - tokenBegin, bufpos);
                bufcolumn = bufcolumnValue;
                maxNextCharInd = bufpos = bufpos + (bufsize - tokenBegin);
            } else {
                System.arraycopy(buffer, tokenBegin, bufferValue, 0, bufsize - tokenBegin);
                buffer = bufferValue;
                System.arraycopy(bufline, tokenBegin, buflineValue, 0, bufsize - tokenBegin);
                bufline = buflineValue;
                System.arraycopy(bufcolumn, tokenBegin, bufcolumnValue, 0, bufsize - tokenBegin);
                bufcolumn = bufcolumnValue;
                maxNextCharInd = bufpos = bufpos - tokenBegin;
            }
        } catch (Throwable throwable) {
            throw new Error(throwable.getMessage());
        }

        bufsize += 2048;
        available = bufsize;
        tokenBegin = 0;
    }

    public void ReInit_v(Reader reader1) {
        this.ReInit(reader1);
    }

    public static void UpdateLineColumn(char ba) {
        column++;
        if (prevCharIsLF) {
            prevCharIsLF = false;
            int lineValue = line;
            column = 1;
            line = lineValue + 1;
        } else if (prevCharIsCR) {
            prevCharIsCR = false;
            if (ba == '\n') {
                prevCharIsLF = true;
            } else {
                int lineValue2 = line;
                column = 1;
                line = lineValue2 + 1;
            }
        }

        switch (ba) {
            case '\t':
                column--;
                column = column + (tabSize - column % tabSize);
                break;
            case '\n':
                prevCharIsLF = true;
            case '\u000b':
            case '\f':
            default:
                break;
            case '\r':
                prevCharIsCR = true;
        }

        bufline[bufpos] = line;
        bufcolumn[bufpos] = column;
    }

    public static int getLine() {
        return bufcolumn[tokenBegin];
    }

    public ChangeLogSimpleCharStream(Reader reader1, int ba, int bb, int bc) {
        if (inputStream != null) {
            throw new Error(SECOND_CONSTRUCTOR_ERROR);
        }

        inputStream = reader1;
        line = 1;
        column = 1 - 1;
        bufsize = 4096;
        available = 4096;
        buffer = new char[4096];
        bufline = new int[4096];
        bufcolumn = new int[4096];
    }

    public static char readChar() throws IOException {
        if (inBuf > 0) {
            inBuf--;
            if (++bufpos == bufsize) {
                bufpos = 0;
            }

            return buffer[bufpos];
        } else {
            if (++bufpos >= maxNextCharInd) {
                FillBuff();
            }

            char bufferElement = buffer[bufpos];
            UpdateLineColumn(bufferElement);
            return bufferElement;
        }
    }

    public static int getEndColumn() {
        return bufline[bufpos];
    }

    public static char BeginToken() throws IOException {
        tokenBegin = -1;
        char ba = readChar();
        tokenBegin = bufpos;
        return ba;
    }

    public ChangeLogSimpleCharStream(Reader reader1) {
        this(reader1, 1, 1, 4096);
    }

    public static void FillBuff() throws IOException {
        if (maxNextCharInd == available) {
            if (available == bufsize) {
                if (tokenBegin > 2048) {
                    maxNextCharInd = 0;
                    bufpos = 0;
                    available = tokenBegin;
                } else if (tokenBegin < 0) {
                    maxNextCharInd = 0;
                    bufpos = 0;
                } else {
                    ExpandBuff(false);
                }
            } else if (available > tokenBegin) {
                available = bufsize;
            } else if (tokenBegin - available < 2048) {
                ExpandBuff(true);
            } else {
                available = tokenBegin;
            }
        }

        try {
            int ba;
            if ((ba = inputStream.read(buffer, maxNextCharInd, available - maxNextCharInd)) == -1) {
                inputStream.close();
                throw new IOException();
            }

            maxNextCharInd += ba;
        } catch (IOException iOException) {
            bufpos--;
            backup(0);
            if (tokenBegin == -1) {
                tokenBegin = bufpos;
            }

            throw iOException;
        }
    }

    public static int getEndLine() {
        return bufline[tokenBegin];
    }
}
