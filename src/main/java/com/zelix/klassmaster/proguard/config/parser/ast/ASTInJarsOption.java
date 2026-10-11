


package com.zelix.klassmaster.proguard.config.parser.ast;

import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.proguard.ProGuardConfigTranslator;
import com.zelix.klassmaster.proguard.config.parser.ast.ASTTildeClause;
import com.zelix.klassmaster.proguard.config.parser.ast.ProGuardOptionNode;
import com.zelix.klassmaster.util.ObservableHolder;

import java.io.IOException;
import java.util.Set;

public class ASTInJarsOption
        extends ProGuardOptionNode {
    public static String getFileExtension(String string) {
        int n = string.lastIndexOf(".");
        if (n > -1 && n < string.length() - 1) {
            return string.substring(n + 1);
        }
        return "";
    }

    public static String buildFileFilterExpression(Set filters, ObservableHolder warning) throws ZkmException, IOException {
        StringBuilder expression = new StringBuilder();
        int filterCount = filters.size();
        int filterIndex = 0;
        boolean previousNegated = false;
        String previousExtension = null;
        expression.append('{');
        for (String rawFilter : (java.lang.Iterable<String>) (Object) filters) {
            String filter = rawFilter.trim();
            boolean negated = false;
            if (filter.startsWith("!")) {
                negated = true;
                filter = filter.substring(1);
            }
            if (filter.startsWith("**")) {
                filter = filter.substring(1);
            }
            if (filterIndex > 0) {
                if (previousNegated && !(negated && filterIndex == filterCount - 1)) {
                    expression.append(" && ");
                    if (!getFileExtension(filter).equals(previousExtension)) {
                        warning.setValue("File filters in ProGuard '-injars' command may be too complicated for translation : '"
                                + rawFilter + "' preceded by negated filter with '" + previousExtension + "' suffix.");
                    }
                } else {
                    expression.append(" || ");
                }
            }
            if (negated) {
                expression.append('!');
                previousNegated = true;
                previousExtension = getFileExtension(filter);
            } else {
                previousNegated = false;
                previousExtension = null;
            }
            expression.append('"');
            expression.append(filter);
            expression.append('"');
            filterIndex++;
        }
        expression.append('}');
        return expression.toString();
    }

    @Override
    public String getOptionName() {
        return "-injars";
    }

    @Override
    public void translateOption(Object object) throws ZkmException, IOException {
        ProGuardConfigTranslator proGuardConfigTranslator = (ProGuardConfigTranslator) object;
        this.jjtGetNumChildren();
        ASTTildeClause aSTTildeClause = (ASTTildeClause) this.jjtGetChild(0);
        Set set = aSTTildeClause.getPaths();
        int setSize = set.size();
        int n2 = 0;
        for (String key : (java.lang.Iterable<String>) (Object) (set)) {
            char c;
            if (!ZkmFileUtils.looksLikeArchiveName(key) && (c = key.charAt(key.length() - 1)) != '*') {
                key = key + (c == ZkmFileUtils.FILE_SEPARATOR_CHAR || c == '/' || c == '\\' ? "" : Character.valueOf(ZkmFileUtils.FILE_SEPARATOR_CHAR)) + "*";
            }
            if (n2 == setSize - 1 && aSTTildeClause.hasFileFilters()) {
                ObservableHolder observableHolder = new ObservableHolder();
                String value = ASTInJarsOption.buildFileFilterExpression(aSTTildeClause.getFileFilters(), observableHolder);
                proGuardConfigTranslator.addOpenArchive(key, value);
                if (!observableHolder.isValueNull()) {
                    proGuardConfigTranslator.logWarning((String) observableHolder.getValue());
                }
            } else {
                proGuardConfigTranslator.addOpenArchive(key, "");
            }
            ++n2;
        }
        if (aSTTildeClause.hasArchiveFilters()) {
            proGuardConfigTranslator.logWarning("Archive level filters not supported in ProGuard '" + this.getOptionName() + "' command : '" + aSTTildeClause.buildArchiveFilterText() + "' ignored in '" + aSTTildeClause.buildClassPathText() + "'");
        }
    }

    public ASTInJarsOption() {
        super(3);
    }
}
