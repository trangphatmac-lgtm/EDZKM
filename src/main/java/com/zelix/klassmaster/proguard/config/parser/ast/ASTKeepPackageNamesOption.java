package com.zelix.klassmaster.proguard.config.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.proguard.ProGuardConfigTranslator;
import com.zelix.klassmaster.proguard.ProGuardWildcardConverter;

import java.io.IOException;

public class ASTKeepPackageNamesOption extends ProGuardOptionNode {
    @Override
    public String getOptionName() {
        return "-keeppackagenames";
    }

    @Override
    public void translateOption(Object object) throws ZkmException, IOException {
        ProGuardConfigTranslator proGuardConfigTranslator = (ProGuardConfigTranslator) object;
        int ba = this.jjtGetNumChildren();
        if (ba > 0) {
            for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
                String value = ((ASTQuote122Clause) this.jjtGetChild(childrenElementIndex)).getValue();
                String string1 = "";
                String string2 = "";
                int characterIndex;
                if ((characterIndex = value.indexOf(":")) == -1 && (characterIndex = value.indexOf("/")) == -1 && (characterIndex = value.indexOf("\\")) == -1) {
                }

                if (characterIndex > -1) {
                    proGuardConfigTranslator.logWarning(
                            "Package name in ProGuard '" + this.getOptionName() + "' command must not contain '" + value.charAt(characterIndex) + "' character : '" + value + "'"
                    );
                } else {
                    if (value.charAt(0) == '!') {
                        value = value.substring(1);
                        string1 = "!(";
                        string2 = ")";
                    }

                    String string3 = ProGuardWildcardConverter.convertPackagePattern(value);
                    proGuardConfigTranslator.addExclude(string1 + string3 + string2);
                }
            }
        } else {
            proGuardConfigTranslator.addExclude("*.");
        }
    }

    public ASTKeepPackageNamesOption() {
        super(45);
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
