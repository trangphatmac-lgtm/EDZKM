package com.zelix.klassmaster.classfile.attribute;

import com.zelix.klassmaster.classfile.constpool.ResolvedStringConstant;

public class ConcatRecipePart {
    public ResolvedStringConstant stringConstant;
    public final int tokenIndex;
    public final String text;

    public String getText() {
        return this.text;
    }

    public ConcatRecipePart(int tokenIndexArgument, String textArgument, ResolvedStringConstant resolvedStringConstant) {
        this(tokenIndexArgument, textArgument);
        this.stringConstant = resolvedStringConstant;
    }

    public ResolvedStringConstant getStringConstant() {
        return this.stringConstant;
    }

    public ConcatRecipePart(int tokenIndex, String textArgument) {
        this.tokenIndex = tokenIndex;
        this.text = textArgument;
    }

    public int getTokenIndex() {
        return this.tokenIndex;
    }

    public void clearStringConstant() {
        this.stringConstant = null;
    }
}
