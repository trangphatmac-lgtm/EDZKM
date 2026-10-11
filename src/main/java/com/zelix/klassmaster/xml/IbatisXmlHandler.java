package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class IbatisXmlHandler extends BeanPropertyXmlHandler {
    public Map typeAliases = ZkmUtils.createHashMap();

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        Map map2 = (Map) object;
        TwoKeyMap twoKeyMap = (TwoKeyMap) object2;
        Map map3 = (Map) object1;
        String tagName = xmlElementNode.getTagName();
        XmlElementNode xmlElementNode1 = this.getOpenElement(0);
        String tagNameValue = null;
        if (xmlElementNode1 != null) {
            tagNameValue = xmlElementNode1.getTagName();
        }

        if (tagName.equalsIgnoreCase("typeAlias")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("alias");
            if (observableHolder != null) {
                String value = (String) observableHolder.getValue();
                if (value != null && value.length() > 0) {
                    ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("type");
                    if (observableHolder1 != null) {
                        String valueValue = (String) observableHolder1.getValue();
                        if (valueValue != null && valueValue.length() > 0) {
                            this.typeAliases.put(value.trim(), valueValue.trim());
                        }
                    }
                }
            }
        } else if (tagName.equalsIgnoreCase("property") && tagNameValue != null && tagNameValue.equalsIgnoreCase("result-map")
                || tagName.equalsIgnoreCase("result") && tagNameValue != null && tagNameValue.equalsIgnoreCase("resultMap")) {
            String string7 = "reference in '" + super.resourceName + "' by '" + tagName + "' tag";
            String string8 = null;
            ObservableHolder observableHolder3 = null;
            if (xmlElementNode1 != null) {
                observableHolder3 = xmlElementNode1.getAttributeValue("class");
            }

            if (observableHolder3 != null) {
                String valueValue2 = (String) observableHolder3.getValue();
                valueValue2 = (String) ZkmUtils.mapOrSelf(valueValue2, this.typeAliases);
                string8 = this.addClassReference(valueValue2, map1, string7 + " '" + "class" + "' attribute");
            }

            if (string8 != null) {
                Enumeration enumeration = xmlElementNode.getAttributeNames();

                while (enumeration.hasMoreElements()) {
                    String key = (String) enumeration.nextElement();
                    String valueValue3 = (String) xmlElementNode.getAttributeValue(key).getValue();
                    String valueValue5 = string7 + " '" + key + "' attribute";
                    if (key.equalsIgnoreCase("name") && tagName.equalsIgnoreCase("property")
                            || key.equalsIgnoreCase("property") && tagName.equalsIgnoreCase("result")) {
                        ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue(key);
                        if (observableHolder2 != null) {
                            String valueValue4 = (String) observableHolder2.getValue();
                            ProgramClass programClass1 = this.findProgramClass(string8);
                            if (programClass1 != null) {
                                this.addPropertyAccessorReferences(programClass1, valueValue4, map3, twoKeyMap, valueValue5);
                            }
                        }
                    }
                }
            }
        } else {
            super.analyzeElement(xmlElementNode, map1, map2, map3, twoKeyMap);
        }
    }

    public IbatisXmlHandler(String string, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1) {
        super(string, classMemberLookup1, classResolver1, messageReporter1);
    }

    public IbatisXmlHandler(
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

    @Override
    public void rewriteElement(Object object, Object object1) throws ZkmException, IOException {
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        rewriteSqlMapReferences(xmlElementNode);
        ((List) object1).add(xmlElementNode);
    }

    private void rewriteSqlMapReferences(XmlElementNode xmlElementNode) throws ZkmException, IOException {
        boolean predicateFlag = XmlConfigFileHandler.getPredicateFlag();
        String tagName = xmlElementNode.getTagName();
        boolean predicateFlagValue = predicateFlag;
        String tagNameValue = null;
        XmlElementNode xmlElementNode1 = this.getOpenElement(0);
        if (xmlElementNode1 != null) {
            tagNameValue = xmlElementNode1.getTagName();
        }

        if (tagName.equalsIgnoreCase("typeAlias")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("alias");
            if (observableHolder != null) {
                String value = (String) observableHolder.getValue();
                if (value != null && value.length() > 0) {
                    ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("type");
                    if (observableHolder1 != null) {
                        String valueValue = (String) observableHolder1.getValue();
                        if (valueValue != null && valueValue.length() > 0) {
                            this.typeAliases.put(value.trim(), valueValue.trim());
                            observableHolder1.setValue(this.getRenamedClassName(valueValue));
                        }
                    }
                }
            }

            if (!predicateFlagValue) {
                return;
            }
        }

        if (tagName.equalsIgnoreCase("property") && tagNameValue != null && tagNameValue.equalsIgnoreCase("result-map")
                || tagName.equalsIgnoreCase("result") && tagNameValue != null && tagNameValue.equalsIgnoreCase("resultMap")) {
            ObservableHolder observableHolder3 = null;
            String string7 = null;
            String renamedClassName = null;
            if (xmlElementNode1 != null) {
                observableHolder3 = xmlElementNode1.getAttributeValue("class");
            }

            if (observableHolder3 != null) {
                String valueValue2 = (String) observableHolder3.getValue();
                string7 = (String) ZkmUtils.mapOrSelf(valueValue2, this.typeAliases);
                renamedClassName = this.getRenamedClassName(string7);
            }

            if (renamedClassName != null) {
                Enumeration enumeration1 = xmlElementNode.getAttributeNames();

                while (enumeration1.hasMoreElements()) {
                    String key = (String) enumeration1.nextElement();
                    String valueValue3 = (String) xmlElementNode.getAttributeValue(key).getValue();
                    if (key.equalsIgnoreCase("name") && tagName.equalsIgnoreCase("property")
                            || key.equalsIgnoreCase("property") && tagName.equalsIgnoreCase("result")) {
                        ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue(key);
                        if (observableHolder2 != null) {
                            String valueValue4 = (String) observableHolder2.getValue();
                            this.resolveRenamedPropertyName(renamedClassName, string7, valueValue4, observableHolder2);
                        }
                    }

                    if (predicateFlagValue) {
                        break;
                    }
                }
            }
        } else {
            Enumeration enumeration = xmlElementNode.getAttributeNames();

            while (enumeration.hasMoreElements()) {
                String keyValue = (String) enumeration.nextElement();
                ObservableHolder observableHolder4 = xmlElementNode.getAttributeValue(keyValue);
                observableHolder4.setValue(this.updateReferenceValue((String) observableHolder4.getValue(), keyValue, false));
            }
        }
    }
}
