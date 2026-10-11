package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class E4XmiXmlHandler extends GenericXmlFileHandler {
    public E4XmiXmlHandler(
            String string,
            EnumerableMap enumerableMap,
            ReadOnlyMultiMap readOnlyMultiMap,
            ReadOnlyMultiMap readOnlyMultiMap1,
            ResourcePathTranslator resourcePathTranslator1,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            MessageReporter messageReporter1
    ) {
        super(string, enumerableMap, readOnlyMultiMap, readOnlyMultiMap1, resourcePathTranslator1, classMemberLookup1, classResolver1, messageReporter1);
    }

    public E4XmiXmlHandler(String string, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1) {
        super(string, classMemberLookup1, classResolver1, messageReporter1);
    }

    @Override
    public void rewriteElement(Object object, Object object1) throws ZkmException, IOException {
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        boolean predicateFlag = XmlConfigFileHandler.getPredicateFlag();
        xmlElementNode.getTagName();
        boolean predicateFlagValue = predicateFlag;
        XmlElementNode xmlElementNode1 = this.getOpenElement(0);
        if (xmlElementNode1 != null) {
            xmlElementNode1.getTagName();
        }

        Enumeration enumeration = xmlElementNode.getAttributeNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue(key);
            if (observableHolder != null) {
                String valueValue = (String) observableHolder.getValue();
                if (valueValue.startsWith("bundleclass://")) {
                    int ba = valueValue.lastIndexOf("/");
                    if (ba > -1 && ba < valueValue.length() - 1) {
                        String string2 = valueValue.substring(ba + 1);
                        ProgramClass programClass1 = this.findProgramClass(string2);
                        if (programClass1 != null && programClass1.isRenamed()) {
                            String string3 = valueValue.substring(0, ba + 1);
                            String valueValue2 = string3 + programClass1.getDottedClassName();
                            observableHolder.setValue(valueValue2);
                        }
                    }

                } else if (valueValue.startsWith("platform:/plugin/")) {
                    int bb = valueValue.indexOf("/", "platform:/plugin/".length());
                    if (bb > -1 && bb < valueValue.length() - 1) {
                        valueValue.substring(0, bb + 1);
                        String string8 = valueValue.substring(bb + 1);
                        int bc = string8.lastIndexOf("/");
                        if (bc > -1) {
                            String string9 = string8.substring(0, bc);
                            String string5 = string8.substring(bc);
                            String string6 = this.renamePackagePath(string9);
                            if (!string9.equals(string6)) {
                                String valueValue3 = valueValue.substring(0, bb + 1) + string6 + string5;
                                observableHolder.setValue(valueValue3);
                            }
                        }
                    }
                }
            }

            if (predicateFlagValue) {
                break;
            }
        }

        ((List) object1).add(xmlElementNode);
    }

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        String tagName = xmlElementNode.getTagName();
        XmlElementNode xmlElementNode1 = this.getOpenElement(0);
        if (xmlElementNode1 != null) {
            xmlElementNode1.getTagName();
        }

        Enumeration enumeration = xmlElementNode.getAttributeNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue(key);
            if (observableHolder != null) {
                String valueValue = (String) observableHolder.getValue();
                if (valueValue.startsWith("bundleclass://")) {
                    int ba = valueValue.lastIndexOf("/");
                    if (ba > -1 && ba < valueValue.length()) {
                        String string3 = valueValue.substring(ba + 1);
                        ProgramClass programClass1 = this.findProgramClass(string3);
                        if (programClass1 != null) {
                            String valueValue2 = "reference in '"
                                    + super.resourceName
                                    + "' by '"
                                    + tagName
                                    + "' tag '"
                                    + key
                                    + "' attribute with value '"
                                    + valueValue
                                    + "'";
                            map1.put(programClass1, valueValue2);
                        }
                    }
                }
            }
        }
    }
}
