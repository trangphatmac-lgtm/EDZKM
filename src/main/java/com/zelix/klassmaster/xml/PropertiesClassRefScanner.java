package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.util.ZkmStringUtils;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;

public class PropertiesClassRefScanner {
    public PropertiesClassRefScanner(BufferedReader bufferedReader, String string, Map map1) throws IOException {
        String string1;
        while ((string1 = bufferedReader.readLine()) != null) {
            int beginIndex = ZkmStringUtils.skipWhitespace(string1, 0);
            String string2;
            if (beginIndex > 0) {
                string2 = string1.substring(beginIndex);
            } else {
                string2 = string1;
            }

            if (!string2.isEmpty() && string2.charAt(0) != '#' && string2.charAt(0) != '!') {
                boolean bl = false;
                StringTokenizer stringTokenizer = new StringTokenizer(string2, " \t=:\\!#", true);

                while (stringTokenizer.hasMoreTokens()) {
                    String string3 = stringTokenizer.nextToken();
                    if (string3.length() == 1) {
                        if (string3.equals(":") || string3.equals("=")) {
                            bl = true;
                        }
                    } else if (bl) {
                        String key = ZkmUtils.dotsToSlashes(string3);
                        if (!key.endsWith(";")
                                && key.indexOf("[") == -1
                                && key.indexOf(".") == -1
                                && key.indexOf("<") == -1
                                && key.indexOf(">") == -1) {
                            ProgramClass programClass1 = ClassHierarchyNode.findProgramClass(key);
                            if (programClass1 != null) {
                                map1.put(programClass1, "Referenced in '" + string + "'");
                            }
                        }
                    }
                }
            }
        }
    }
}
