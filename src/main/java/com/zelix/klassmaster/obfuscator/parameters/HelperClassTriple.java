package com.zelix.klassmaster.obfuscator.parameters;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.util.ObjectTriple;

public class HelperClassTriple implements LookupClassOption {
    public ObjectTriple helperClasses;
    public final ObjectTriple helperClassNames;

    public String getFirstClassName() {
        return (String) this.helperClassNames.getFirst();
    }

    public HelperClassTriple(ObjectTriple helperClassNames) {
        this.helperClassNames = helperClassNames;
    }

    public void setHelperClass(ProgramClass helperClass, int helperIndex) {
        switch (helperIndex) {
            case 0:
                this.helperClasses = new ObjectTriple(helperClass, null, null);
                break;
            case 1:
                this.helperClasses.setSecond(helperClass);
                break;
            case 2:
                this.helperClasses.setThird(helperClass);
        }
    }

    public String getThirdClassName() {
        return (String) this.helperClassNames.getThird();
    }

    public int indexOfClassName(String className) {
        if (((String) this.helperClassNames.getFirst()).equals(className)) {
            return 0;
        } else {
            return ((String) this.helperClassNames.getSecond()).equals(className) ? 1 : 2;
        }
    }

    @Override
    public boolean shouldMapClassesToLookupClass() {
        return false;
    }

    public String getSecondClassName() {
        return (String) this.helperClassNames.getSecond();
    }
}
