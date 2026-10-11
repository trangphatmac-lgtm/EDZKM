package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.archive.ArchivePathFilter;
import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.script.FileFilterComponentSink;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ZkmStringUtils;

import java.io.IOException;
import java.util.zip.ZipException;

public abstract class FileFilterNodeBase extends ZkmScriptSimpleNode implements ArchivePathFilter {
    public String fileNamePattern;
    public String patternText;
    public String directoryPattern;
    public final boolean negated;

    public String normalizePath(String string) {
        String string1 = string.replace('\\', '/');
        boolean caseSensitiveFileSystemValue;
        if (ZkmFileUtils.FILE_SEPARATOR_CHAR != '\\') {
            string1 = string.replace(ZkmFileUtils.FILE_SEPARATOR_CHAR, '/');
            caseSensitiveFileSystemValue = ZkmFileUtils.caseSensitiveFileSystem;
        } else {
            caseSensitiveFileSystemValue = ZkmFileUtils.caseSensitiveFileSystem;
        }

        if (!caseSensitiveFileSystemValue) {
            string1 = string1.toUpperCase();
        }

        return string1;
    }

    public FileFilterNodeBase(int nodeTypeId, boolean negated) {
        super(nodeTypeId);
        this.negated = negated;
    }

    @Override
    public boolean acceptsPath(Object path) {
        String normalizedPath = this.normalizePath((String) path);
        int separatorIndex = lastSeparatorIndex(normalizedPath);
        String fileName = normalizedPath.substring(separatorIndex + 1);
        String directory = normalizedPath.substring(0, separatorIndex + 1);
        boolean matches = ZkmStringUtils.matchesWildcard(directory, this.directoryPattern)
                && ZkmStringUtils.matchesWildcard(fileName, this.fileNamePattern);
        return this.negated ? !matches : matches;
    }

    public static int lastSeparatorIndex(String string) {
        int ba = string.lastIndexOf(47);
        int bb = string.lastIndexOf("!");
        return Math.max(ba, bb);
    }

    @Override
    public String toFilterExpression() throws ZipException {
        return (this.negated ? "!" : "") + "\"" + this.patternText + "\"";
    }

    @Override
    public final void execute(Object object, Object object1) throws ZkmException, IOException {
        this.jjtGetNumChildren();
        ZkmScriptASTStringLiteral zkmScriptASTStringLiteral = (ZkmScriptASTStringLiteral) this.jjtGetChild(0);
        this.patternText = zkmScriptASTStringLiteral.getValue();
        String string = this.normalizePath(this.patternText);
        int ba = lastSeparatorIndex(string);
        this.directoryPattern = string.substring(0, ba + 1);
        this.directoryPattern = "*" + this.directoryPattern;
        this.fileNamePattern = string.substring(ba + 1);
        ((FileFilterComponentSink) object).addFileFilterComponent(this);
    }
}
