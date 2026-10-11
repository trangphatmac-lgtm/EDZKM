package com.zelix.klassmaster.proguard;

import com.zelix.klassmaster.util.StringKeyTransform;

public class LineCommentStripper implements StringKeyTransform {
    private static final String COMMENT_PREFIX = "//";

    @Override
    public String transformLine(String string) {
        int endIndex = string.indexOf(COMMENT_PREFIX);
        return endIndex > -1 ? string.substring(0, endIndex) : string;
    }
}
