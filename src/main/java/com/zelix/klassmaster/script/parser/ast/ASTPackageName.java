package com.zelix.klassmaster.script.parser.ast;

import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.obfuscator.exclude.QualifiedNameMatcher;
import com.zelix.klassmaster.script.parser.ZkmScriptSimpleNode;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ASTPackageName extends ZkmScriptSimpleNode implements QualifiedNameMatcher {
    public String dottedName;
    private String internalName;
    public ArrayList nameComponents = new ArrayList();
    private final String subpackageWildcardSuffix = "/*";

    @Override
    public void execute(Object object, Object object1) throws ZkmException, IOException {
        int ba = this.jjtGetNumChildren();
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder1 = new StringBuilder();

        for (int childrenElementIndex = 0; childrenElementIndex < ba; childrenElementIndex++) {
            String value = ((ScriptValueNode) this.jjtGetChild(childrenElementIndex)).getValue();
            this.nameComponents.add(value);
            stringBuilder.append(value);
            stringBuilder.append(".");
            stringBuilder1.append(value);
            if (childrenElementIndex < ba - 1) {
                stringBuilder1.append("/");
            }
        }

        this.dottedName = stringBuilder.toString();
        this.internalName = stringBuilder1.toString();
    }

    public ASTPackageName() {
        super(191);
    }

    public boolean isLiteralName() {
        return this.dottedName.indexOf("*") == -1;
    }

    @Override
    public String getSpecText() {
        return this.dottedName;
    }

    @Override
    public boolean matchesName(String string) {
        boolean bl = ZkmStringUtils.matchesWildcard(string, this.internalName);
        if (!bl && ((ASTComplexPackageSpecifier) super.parent).hasDotSuffix()) {
            String string1 = this.internalName + "/*";
            bl = ZkmStringUtils.matchesWildcard(string, string1);
        }

        return bl;
    }

    public List getNameComponents() {
        return ZkmUtils.copyArrayList(this.nameComponents);
    }
}
