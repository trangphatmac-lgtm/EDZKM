package com.zelix.klassmaster.changelog;

import com.zelix.klassmaster.classfile.constpool.ConstantPoolEntry;
import com.zelix.klassmaster.util.MutableInt;
import com.zelix.klassmaster.util.ZkmAssert;

import java.util.StringTokenizer;

public class StackTraceFrameLine implements StackTraceLineInfo {
    public String methodName;
    public MutableInt lineNumber;
    public String className;
    public String methodDescriptor;
    public String errorMessage;
    public String modulePrefix;
    public String leadingText = "";
    public boolean parsed = true;
    public String locationPrefix = "";
    public String locationSuffix = "";
    public String lineText;

    public String getMethodName() {
        return this.methodName;
    }

    public boolean parseQualifiedMethodName(String string) {
        String string1 = string;
        int beginIndex = string1.indexOf("/");
        if (beginIndex > -1) {
            ZkmAssert.assertTrue(beginIndex > 0 && beginIndex < string1.length() - 2, new String[]{"Invalid stack trace line '" + string1 + "' : " + beginIndex + " : '" + string1 + "'"});
            this.modulePrefix = string1.substring(beginIndex);
            string1 = string1.substring(beginIndex + 1);
        }

        StringTokenizer stringTokenizer = new StringTokenizer(string1, ".");
        int bb = stringTokenizer.countTokens();
        if (bb > 1) {
            int bc = 0;
            StringBuilder stringBuilder = new StringBuilder();

            while (stringTokenizer.hasMoreTokens()) {
                String string2 = stringTokenizer.nextToken();
                if (bc == 0) {
                    stringBuilder.append(string2);
                } else if (bc == bb - 1) {
                    this.methodName = string2.trim();
                    int endIndex = this.methodName.indexOf(" ");
                    if (endIndex > -1) {
                        this.methodName = this.methodName.substring(0, endIndex);
                    }
                } else {
                    stringBuilder.append("." + string2);
                }

                bc++;
            }

            this.className = stringBuilder.toString();
            this.className = this.className.replace('/', '.');
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getClassName() {
        return this.className != null ? this.className.replace('/', '.') : null;
    }

    public StackTraceFrameLine(String string) {
        this.lineText = string.trim();
        int endIndex = this.findAtIndex();
        if (endIndex > -1) {
            this.leadingText = this.lineText.substring(0, endIndex);
            String string1 = this.lineText.substring(endIndex + "at ".length()).trim();
            int bb = string1.indexOf("(");
            if (bb > -1) {
                if (this.parseQualifiedMethodName(string1.substring(0, bb).trim())) {
                    this.parseLocation(string1.substring(bb));
                } else {
                    this.parsed = false;
                }
            } else {
                this.parsed = this.parseQualifiedMethodName(string1.trim());
            }
        } else {
            this.parsed = false;
        }
    }

    @Override
    public boolean hasLineNumber() {
        return this.lineNumber != null;
    }

    @Override
    public String getLineText() {
        return this.lineText;
    }

    public boolean hasError() {
        return this.errorMessage != null;
    }

    public void parseLocation(String string) {
        if (string.length() >= 2) {
            String locationPrefixValue = string;
            int ba = locationPrefixValue.indexOf("(", 1);
            int bb = locationPrefixValue.indexOf(")");
            if (ba > bb) {
                String methodDescriptorValue = locationPrefixValue.substring(0, ba).trim();
                if (ConstantPoolEntry.isValidMethodDescriptor(methodDescriptorValue)) {
                    this.methodDescriptor = methodDescriptorValue;
                    locationPrefixValue = locationPrefixValue.substring(ba);
                }
            }

            int be = locationPrefixValue.indexOf(":");
            int bc = locationPrefixValue.lastIndexOf(")");
            if (be > -1) {
                if (bc > -1) {
                    if (bc > be) {
                        String string3 = locationPrefixValue.substring(be + 1, bc).trim();

                        try {
                            int value = Integer.parseInt(string3);
                            this.lineNumber = new MutableInt(value);
                            this.locationPrefix = locationPrefixValue.substring(0, be + 1);
                            this.locationSuffix = locationPrefixValue.substring(bc);
                        } catch (NumberFormatException numberFormatException) {
                            this.locationPrefix = locationPrefixValue;
                        }
                    } else {
                        this.locationPrefix = locationPrefixValue;
                    }
                } else {
                    this.locationPrefix = locationPrefixValue;
                }
            } else {
                this.locationPrefix = locationPrefixValue;
            }
        }
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public int findAtIndex() {
        boolean bl = false;
        int ba = -1;
        int bb = -1;

        while (!bl && (bb = this.lineText.indexOf("at ", bb + 1)) > -1) {
            if (bb <= 0) {
                ba = 0;
                break;
            }

            switch (this.lineText.charAt(bb - 1)) {
                case '\t':
                case ' ':
                    bl = true;
                    ba = bb;
            }
        }

        return ba;
    }

    @Override
    public boolean isValidFrame() {
        return this.parsed && this.errorMessage == null;
    }

    public String getMethodDescriptor() {
        return this.methodDescriptor;
    }

    public void setErrorMessage(String errorMessageArgument) {
        this.errorMessage = errorMessageArgument;
    }

    @Override
    public int getLineNumber(Object object, Object object1) {
        return this.lineNumber != null ? this.lineNumber.getValue() : -1;
    }
}
