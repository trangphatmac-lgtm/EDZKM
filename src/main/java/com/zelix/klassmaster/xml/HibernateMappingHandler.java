package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.classfile.AbstractFieldInfo;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HibernateMappingHandler extends BeanPropertyXmlHandler {
    public static Set builtInGeneratorNames;
    public String defaultPackage;
    public String currentClassName;
    public ProgramClass currentClass;
    public boolean useFieldAccess;

    public ProgramClass updateClassAttribute(XmlElementNode xmlElementNode, String keyArgument) throws ZkmException, IOException {
        ProgramClass programClass1 = null;
        ObservableHolder observableHolder = xmlElementNode.getAttributeValue(keyArgument);
        if (observableHolder != null) {
            String value = (String) observableHolder.getValue();
            programClass1 = this.findProgramClass(value);
            boolean bl = false;
            if (programClass1 == null && this.defaultPackage != null) {
                programClass1 = this.findClassInPackage(value, this.defaultPackage);
                bl = programClass1 != null;
            }

            if (programClass1 != null) {
                String string2;
                if (bl) {
                    string2 = programClass1.getSimpleName();
                } else {
                    string2 = programClass1.getDottedClassName();
                }

                observableHolder.setValue(string2);
            }
        }

        return programClass1;
    }

    public HibernateMappingHandler(String string, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1) {
        super(string, classMemberLookup1, classResolver1, messageReporter1);
    }

    public ProgramClass addClassAttributeReference(XmlElementNode xmlElementNode, String string, String keyArgument, String string2, Map map1) throws ZkmException, IOException {
        Map map2 = map1;
        String value = string2;
        return this.resolveClassAttribute(xmlElementNode, string, keyArgument, (ObservableHolder) null, value, map2);
    }

    public ProgramClass findClassInPackage(String string, String string1) {
        ProgramClass programClass1 = this.findProgramClass(string);
        if (programClass1 == null && string1 != null) {
            programClass1 = this.findProgramClass(string1 + "." + string);
        }

        return programClass1;
    }

    public ProgramClass addClassAttributeReference(XmlElementNode xmlElementNode, String string, String keyArgument, Map map1) throws ZkmException, IOException {
        if (xmlElementNode.getAttributeValue(keyArgument) != null) {
            String tagNameValue = xmlElementNode.getTagName();
            String string3 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + keyArgument + "' attribute";
            return this.addClassAttributeReference(xmlElementNode, string, keyArgument, string3, map1);
        } else {
            return null;
        }
    }

    public ProgramClass resolveClassAttribute(
            XmlElementNode xmlElementNode, String string, String keyArgument, ObservableHolder observableHolder, Object value, Map map1
    ) throws ZkmException, IOException {
        ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue(keyArgument);
        ProgramClass programClass1 = null;
        if (observableHolder1 != null) {
            String valueValue = (String) observableHolder1.getValue();
            if (observableHolder != null) {
                observableHolder.setValue(valueValue);
            }

            programClass1 = this.findProgramClass(valueValue);
            if (programClass1 == null && string != null) {
                programClass1 = this.findClassInPackage(valueValue, string);
            }

            if (programClass1 != null) {
                map1.put(programClass1, value);
            }
        }

        return programClass1;
    }

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        Map map2 = (Map) object1;
        TwoKeyMap twoKeyMap = (TwoKeyMap) object2;
        Map map3 = (Map) object;
        String tagNameValue = xmlElementNode.getTagName();
        if (tagNameValue.equalsIgnoreCase("hibernate-mapping")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("package");
            XmlElementNode xmlElementNode3;
            String string22;
            if (observableHolder != null) {
                String defaultPackageValue = (String) observableHolder.getValue();
                if (defaultPackageValue != null) {
                    if (defaultPackageValue.length() > 0) {
                        this.defaultPackage = defaultPackageValue;
                        xmlElementNode3 = xmlElementNode;
                        long bd = 24856730383790L;
                        string22 = "default-access";
                    } else {
                        xmlElementNode3 = xmlElementNode;
                        long ba = 24856730383790L;
                        string22 = "default-access";
                    }
                } else {
                    xmlElementNode3 = xmlElementNode;
                    long bb = 24856730383790L;
                    string22 = "default-access";
                }
            } else {
                xmlElementNode3 = xmlElementNode;
                long bc = 24856730383790L;
                string22 = "default-access";
            }

            String keyValue = string22;
            ObservableHolder observableHolder3 = xmlElementNode3.getAttributeValue(keyValue);
            if (observableHolder3 != null) {
                String value = (String) observableHolder3.getValue();
                if (value != null) {
                    this.useFieldAccess = value.equals("field");
                }
            }
        } else if (tagNameValue.equalsIgnoreCase("class")) {
            String valueValue4 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "name" + "' attribute";
            ObservableHolder observableHolder4 = new ObservableHolder();
            ProgramClass programClass3 = this.resolveClassAttribute(xmlElementNode, this.defaultPackage, "name", observableHolder4, valueValue4, map1);
            if (!observableHolder4.isValueNull()) {
                this.currentClassName = (String) observableHolder4.getValue();
            }

            if (programClass3 != null) {
                this.currentClass = programClass3;
            }

            observableHolder4.setValue(null);
            String valueValue5 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "persister" + "' attribute";
            this.resolveClassAttribute(xmlElementNode, this.defaultPackage, "persister", observableHolder4, valueValue5, map1);
            String valueValue6 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "entity-name" + "' attribute";
            this.resolveClassAttribute(xmlElementNode, this.defaultPackage, "entity-name", observableHolder4, valueValue6, map1);
        } else {
            HibernateMappingHandler hibernateMappingHandler1;
            XmlElementNode xmlElementNode2;
            String keyValue3;
            if (!tagNameValue.equalsIgnoreCase("id")) {
                if (!tagNameValue.equalsIgnoreCase("property")) {
                    if (!tagNameValue.equalsIgnoreCase("many-to-one")) {
                        if (!tagNameValue.equalsIgnoreCase("one-to-one")) {
                            if (!tagNameValue.equalsIgnoreCase("composite-id")) {
                                if (tagNameValue.equalsIgnoreCase("one-to-many") || tagNameValue.equalsIgnoreCase("map-key-many-to-many")) {
                                    String string8 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "class" + "' attribute";
                                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "class", string8, map1);
                                    String string11 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "entity-name" + "' attribute";
                                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "entity-name", string11, map1);
                                    return;
                                }

                                if (tagNameValue.equalsIgnoreCase("many-to-many")) {
                                    ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("entity-name");
                                    if (observableHolder1 != null && !observableHolder1.isValueNull()) {
                                        String string9 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "entity-name" + "' attribute";
                                        this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "entity-name", string9, map1);
                                        return;
                                    }

                                    return;
                                } else {
                                    if (tagNameValue.equalsIgnoreCase("generator")) {
                                        String string7 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "class" + "' attribute";
                                        ObservableHolder observableHolder5 = xmlElementNode.getAttributeValue("class");
                                        if (observableHolder5 != null) {
                                            String valueValue = (String) observableHolder5.getValue();
                                            if (!builtInGeneratorNames.contains(valueValue)) {
                                                this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "class", string7, map1);
                                            }

                                            return;
                                        }
                                    } else {
                                        if (!tagNameValue.equalsIgnoreCase("set")) {
                                            if (!tagNameValue.equalsIgnoreCase("list")) {
                                                if (!tagNameValue.equalsIgnoreCase("map")) {
                                                    if (!tagNameValue.equalsIgnoreCase("bag")) {
                                                        if (!tagNameValue.equalsIgnoreCase("array")) {
                                                            if (!tagNameValue.equalsIgnoreCase("primitive-array")) {
                                                                if (!tagNameValue.equalsIgnoreCase("subclass")
                                                                        && !tagNameValue.equalsIgnoreCase("joined-subclass")
                                                                        && !tagNameValue.equalsIgnoreCase("union-subclass")) {
                                                                    super.analyzeElement(xmlElementNode, map1, map3, map2, twoKeyMap);
                                                                } else {
                                                                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "name", map1);
                                                                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "extends", map1);
                                                                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "entity-name", map1);
                                                                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "persister", map1);
                                                                }

                                                                return;
                                                            }

                                                            hibernateMappingHandler1 = this;
                                                            xmlElementNode2 = xmlElementNode;
                                                            keyValue3 = "name";
                                                        } else {
                                                            hibernateMappingHandler1 = this;
                                                            xmlElementNode2 = xmlElementNode;
                                                            keyValue3 = "name";
                                                        }
                                                    } else {
                                                        hibernateMappingHandler1 = this;
                                                        xmlElementNode2 = xmlElementNode;
                                                        keyValue3 = "name";
                                                    }
                                                } else {
                                                    hibernateMappingHandler1 = this;
                                                    xmlElementNode2 = xmlElementNode;
                                                    keyValue3 = "name";
                                                }
                                            } else {
                                                hibernateMappingHandler1 = this;
                                                xmlElementNode2 = xmlElementNode;
                                                keyValue3 = "name";
                                            }
                                        } else {
                                            hibernateMappingHandler1 = this;
                                            xmlElementNode2 = xmlElementNode;
                                            keyValue3 = "name";
                                        }

                                        hibernateMappingHandler1.addPropertyReference(xmlElementNode2, keyValue3, map3, map2, twoKeyMap);
                                        ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue("sort");
                                        if (observableHolder2 != null
                                                && !observableHolder2.isValueNull()
                                                && !((String) observableHolder2.getValue()).equals("unsorted")
                                                && !((String) observableHolder2.getValue()).equals("natural")) {
                                            String string10 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "sort" + "' attribute";
                                            this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "sort", string10, map1);
                                            return;
                                        }
                                    }

                                    return;
                                }
                            }

                            hibernateMappingHandler1 = this;
                            xmlElementNode2 = xmlElementNode;
                            keyValue3 = "name";
                        } else {
                            hibernateMappingHandler1 = this;
                            xmlElementNode2 = xmlElementNode;
                            keyValue3 = "name";
                        }
                    } else {
                        hibernateMappingHandler1 = this;
                        xmlElementNode2 = xmlElementNode;
                        keyValue3 = "name";
                    }
                } else {
                    hibernateMappingHandler1 = this;
                    xmlElementNode2 = xmlElementNode;
                    keyValue3 = "name";
                }
            } else {
                hibernateMappingHandler1 = this;
                xmlElementNode2 = xmlElementNode;
                keyValue3 = "name";
            }

            hibernateMappingHandler1.addPropertyReference(xmlElementNode2, keyValue3, map3, map2, twoKeyMap);
            ArrayList arrayList = new ArrayList();
            if (xmlElementNode.getAttributeValue("class") != null) {
                String string12 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "class" + "' attribute";
                ProgramClass programClass4 = this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "class", string12, map1);
                if (programClass4 != null) {
                    arrayList.add(programClass4);
                }
            }

            if ((tagNameValue.equalsIgnoreCase("many-to-one") || tagNameValue.equalsIgnoreCase("one-to-one")) && xmlElementNode.getAttributeValue("entity-name") != null) {
                String string13 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "entity-name" + "' attribute";
                ProgramClass programClass5 = this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "entity-name", string13, map1);
                if (programClass5 != null) {
                    arrayList.add(programClass5);
                }
            }

            XmlElementNode xmlElementNode1;
            String keyValue2;
            if (!tagNameValue.equalsIgnoreCase("property")) {
                if (xmlElementNode.getAttributeValue("access") != null) {
                    String string14 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "access" + "' attribute";
                    this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "access", string14, map1);
                    xmlElementNode1 = xmlElementNode;
                    keyValue2 = "type";
                } else {
                    xmlElementNode1 = xmlElementNode;
                    keyValue2 = "type";
                }
            } else {
                xmlElementNode1 = xmlElementNode;
                keyValue2 = "type";
            }

            if (xmlElementNode1.getAttributeValue(keyValue2) != null) {
                String string15 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "type" + "' attribute";
                this.addClassAttributeReference(xmlElementNode, this.defaultPackage, "type", string15, map1);
            }

            if ((tagNameValue.equalsIgnoreCase("one-to-one") || tagNameValue.equalsIgnoreCase("many-to-one")) && xmlElementNode.getAttributeValue("property-ref") != null) {
                ObservableHolder observableHolder6 = xmlElementNode.getAttributeValue("property-ref");
                if (observableHolder6 != null && !observableHolder6.isValueNull()) {
                    String valueValue2 = (String) observableHolder6.getValue();
                    String valueValue7 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "property-ref" + "' attribute";
                    if (arrayList.size() > 0) {
                        if (!this.useFieldAccess) {
                            Iterator iterator = arrayList.iterator();

                            while (iterator.hasNext()) {
                                ProgramClass programClass1 = (ProgramClass) iterator.next();
                                this.addPropertyAccessorReferences(programClass1, valueValue2, map2, twoKeyMap, valueValue7);
                            }
                        } else {
                            Iterator iterator1 = arrayList.iterator();

                            while (iterator1.hasNext()) {
                                ProgramClass programClass6 = (ProgramClass) iterator1.next();
                                this.addFieldReference(programClass6, valueValue2, map3, valueValue7);
                            }
                        }
                    } else if (xmlElementNode.getAttributeValue("name") != null && this.currentClass != null) {
                        String valueValue3 = (String) xmlElementNode.getAttributeValue("name").getValue();
                        AbstractFieldInfo[] abstractFieldInfos = this.currentClass.findFieldsByName(valueValue3);
                        if (abstractFieldInfos.length == 1) {
                            ProgramClass programClass2 = this.findProgramClass(abstractFieldInfos[0].getTypeName());
                            if (programClass2 != null) {
                                if (!this.useFieldAccess) {
                                    this.addPropertyAccessorReferences(programClass2, valueValue2, map2, twoKeyMap, valueValue7);
                                } else {
                                    this.addFieldReference(programClass2, valueValue2, map3, valueValue7);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void addPropertyReference(XmlElementNode xmlElementNode, String keyArgument, Map map1, Map map2, TwoKeyMap twoKeyMap) throws ZkmException, IOException {
        ObservableHolder observableHolder = xmlElementNode.getAttributeValue(keyArgument);
        if (observableHolder != null) {
            String value = (String) observableHolder.getValue();
            String tagNameValue = xmlElementNode.getTagName();
            if (this.currentClass != null) {
                String valueValue = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + keyArgument + "' attribute";
                if (!this.useFieldAccess) {
                    this.addPropertyAccessorReferences(this.currentClass, value, map2, twoKeyMap, valueValue);
                } else {
                    this.addFieldReference(this.currentClass, value, map1, valueValue);
                }
            }
        }
    }

    public void updatePropertyName(String string, String string1, ObservableHolder observableHolder, ProgramClass programClass1) throws ZkmException, IOException {
        String dottedClassName = programClass1.getDottedClassName();
        if (!this.useFieldAccess) {
            String propertyNameFromAccessorValue3 = null;
            String string4 = XmlConfigFileHandler.buildAccessorName("set", string1);
            String string5 = this.lookupRenamedMethodName(dottedClassName, string4, BeanPropertyXmlHandler.SETTER_DESCRIPTOR, 1);
            if (string5 == null && string.equalsIgnoreCase("id")) {
                string5 = this.lookupRenamedMethodName(dottedClassName, string4, BeanPropertyXmlHandler.SETTER_DESCRIPTOR, 2);
            }

            String string14;
            if (string5 != null) {
                String propertyNameFromAccessor = this.getPropertyNameFromAccessor("set", string5, string1);
                if (propertyNameFromAccessor == null) {
                    super.messageReporter
                            .reportError(
                                    "ERROR:",
                                    "Could not retain bean format for property '"
                                            + string1
                                            + "' in  XML file '"
                                            + super.resourceName
                                            + "' : '"
                                            + string4
                                            + "' '"
                                            + string5
                                            + "' : '"
                                            + programClass1.getDisplayLocationName()
                                            + "' (4)"
                            );
                    string14 = "get";
                } else {
                    propertyNameFromAccessorValue3 = propertyNameFromAccessor;
                    observableHolder.setValue(propertyNameFromAccessor);
                    string14 = "get";
                }
            } else {
                string14 = "get";
            }

            String string12 = XmlConfigFileHandler.buildAccessorName(string14, string1);
            String string7 = this.lookupRenamedMethodName(dottedClassName, string12, BeanPropertyXmlHandler.GETTER_DESCRIPTOR, 1);
            if (string7 != null) {
                String propertyNameFromAccessorValue = this.getPropertyNameFromAccessor("get", string7, string1);
                if (propertyNameFromAccessorValue == null) {
                    super.messageReporter
                            .reportError(
                                    "ERROR:",
                                    "Could not retain bean format for property '"
                                            + string1
                                            + "' in  XML file '"
                                            + super.resourceName
                                            + "' : '"
                                            + string12
                                            + "' '"
                                            + string7
                                            + "' : '"
                                            + programClass1.getDisplayLocationName()
                                            + "' (5)"
                            );
                } else {
                    if (propertyNameFromAccessorValue3 == null) {
                        propertyNameFromAccessorValue3 = propertyNameFromAccessorValue;
                    } else {
                        boolean bl = propertyNameFromAccessorValue3.equals(propertyNameFromAccessorValue);
                        String[] strings = new String[]{
                                "Could not retain bean format for property '"
                                        + string1
                                        + "' in  XML file '"
                                        + super.resourceName
                                        + "' : Clashing new values '"
                                        + propertyNameFromAccessorValue3
                                        + "' '"
                                        + propertyNameFromAccessorValue
                                        + "' : '"
                                        + programClass1.getDisplayLocationName()
                                        + "' (A)"
                        };
                        ZkmAssert.assertTrue(bl, strings);
                    }

                    observableHolder.setValue(propertyNameFromAccessorValue);
                }
            }

            String string13 = XmlConfigFileHandler.buildAccessorName("is", string1);
            String string9 = this.lookupRenamedMethodName(
                    dottedClassName,
                    string13,
                    HiddenOptionFlags.USE_BOOLEAN_IS_GETTER_TYPE ? BeanPropertyXmlHandler.BOOLEAN_GETTER_DESCRIPTOR : BeanPropertyXmlHandler.GETTER_DESCRIPTOR,
                    1
            );
            if (string9 != null) {
                String propertyNameFromAccessorValue2 = this.getPropertyNameFromAccessor("is", string9, string1);
                if (propertyNameFromAccessorValue2 == null) {
                    super.messageReporter
                            .reportError(
                                    "ERROR:",
                                    "Could not retain bean format for property '"
                                            + string1
                                            + "' in  XML file '"
                                            + super.resourceName
                                            + "' : '"
                                            + string13
                                            + "' '"
                                            + string9
                                            + "' : '"
                                            + programClass1.getDisplayLocationName()
                                            + "' (6)"
                            );
                } else {
                    if (propertyNameFromAccessorValue3 != null) {
                        boolean bl1 = propertyNameFromAccessorValue3.equals(propertyNameFromAccessorValue2);
                        String[] strings1 = new String[]{
                                "Could not retain bean format for property '"
                                        + string1
                                        + "' in  XML file '"
                                        + super.resourceName
                                        + "' : Clashing new values '"
                                        + propertyNameFromAccessorValue3
                                        + "' '"
                                        + propertyNameFromAccessorValue2
                                        + "' : '"
                                        + programClass1.getDisplayLocationName()
                                        + "' (B)"
                        };
                        ZkmAssert.assertTrue(bl1, strings1);
                    }

                    observableHolder.setValue(propertyNameFromAccessorValue2);
                }
            }
        } else {
            String renamedFieldName = this.getRenamedFieldName(dottedClassName, string1);
            observableHolder.setValue(renamedFieldName);
        }
    }

    @Override
    public void rewriteElement(Object element, Object output) throws ZkmException, IOException {
        XmlElementNode node = (XmlElementNode) element;
        rewriteMappingReferences(node);
        ((List) output).add(node);
    }

    private void rewriteMappingReferences(XmlElementNode node) throws ZkmException, IOException {
        boolean continueAfterMatch = XmlConfigFileHandler.getPredicateFlag();
        String tagName = node.getTagName();
        if (tagName.equalsIgnoreCase("hibernate-mapping")) {
            ObservableHolder packageAttribute = node.getAttributeValue("package");
            if (packageAttribute != null) {
                String packageName = (String) packageAttribute.getValue();
                if (packageName != null && packageName.length() > 0) {
                    this.defaultPackage = packageName;
                    packageAttribute.setValue(this.renameDirectoryPath(packageName));
                }
            }
            ObservableHolder accessAttribute = node.getAttributeValue("default-access");
            if (accessAttribute != null) {
                String accessMode = (String) accessAttribute.getValue();
                if (accessMode != null) {
                    this.useFieldAccess = accessMode.equals("field");
                }
            }
            return;
        }
        if (tagName.equalsIgnoreCase("class")) {
            ProgramClass mappedClass = this.updateClassAttribute(node, "name");
            if (mappedClass != null) {
                this.currentClass = mappedClass;
            }
            this.updateClassAttribute(node, "persister");
            this.updateClassAttribute(node, "entity-name");
            return;
        }

        if (tagName.equalsIgnoreCase("id")
                || tagName.equalsIgnoreCase("property")
                || tagName.equalsIgnoreCase("many-to-one")
                || tagName.equalsIgnoreCase("one-to-one")
                || tagName.equalsIgnoreCase("composite-id")) {
            this.updateClassAttribute(node, "entity-name");
            ObservableHolder nameAttribute = node.getAttributeValue("name");
            if (nameAttribute != null) {
                String propertyName = (String) nameAttribute.getValue();
                if (this.currentClass != null) {
                    this.updatePropertyName(tagName, propertyName, nameAttribute, this.currentClass);
                }
            }

            ProgramClass referencedClass = this.updateClassAttribute(node, "class");
            this.updateClassAttribute(node, "type");
            this.updateClassAttribute(node, "access");
            if ((tagName.equalsIgnoreCase("one-to-one") || tagName.equalsIgnoreCase("many-to-one"))
                    && node.getAttributeValue("property-ref") != null) {
                ObservableHolder propertyReference = node.getAttributeValue("property-ref");
                if (propertyReference != null && !propertyReference.isValueNull()) {
                    String propertyName = (String) propertyReference.getValue();
                    if (referencedClass != null) {
                        this.updatePropertyName(tagName, propertyName, propertyReference, referencedClass);
                    } else if (node.getAttributeValue("name") != null && this.currentClass != null) {
                        String fieldName = this.getRenamedFieldName(this.currentClass.getClassName(), (String) node.getAttributeValue("name").getValue());
                        AbstractFieldInfo[] matchingFields = this.currentClass.findFieldsByName(fieldName);
                        if (matchingFields.length == 1) {
                            ProgramClass fieldClass = this.findProgramClass(matchingFields[0].getTypeName());
                            if (fieldClass != null) {
                                this.updatePropertyName(tagName, propertyName, propertyReference, fieldClass);
                            }
                        }
                    }
                }
            }
            if (!continueAfterMatch) {
                return;
            }
        }

        if (tagName.equalsIgnoreCase("generator")) {
            this.updateClassAttribute(node, "class");
            this.updateClassAttribute(node, "entity-name");
            return;
        }
        if (tagName.equalsIgnoreCase("one-to-many") || tagName.equalsIgnoreCase("map-key-many-to-many")) {
            this.updateClassAttribute(node, "class");
            this.updateClassAttribute(node, "entity-name");
            if (!continueAfterMatch) {
                return;
            }
        }
        if (tagName.equalsIgnoreCase("many-to-many")) {
            this.updateClassAttribute(node, "entity-name");
            if (!continueAfterMatch) {
                return;
            }
        }
        if (tagName.equalsIgnoreCase("set")
                || tagName.equalsIgnoreCase("list")
                || tagName.equalsIgnoreCase("map")
                || tagName.equalsIgnoreCase("bag")
                || tagName.equalsIgnoreCase("array")
                || tagName.equalsIgnoreCase("primitive-array")) {
            if (this.currentClass != null) {
                this.updatePropertyAttribute(node, this.currentClass);
            }
            ObservableHolder sortAttribute = node.getAttributeValue("sort");
            if (sortAttribute != null
                    && !sortAttribute.isValueNull()
                    && !((String) sortAttribute.getValue()).equals("unsorted")
                    && !((String) sortAttribute.getValue()).equals("natural")) {
                this.updateClassAttribute(node, "sort");
            }
            if (!continueAfterMatch) {
                return;
            }
        }
        if (tagName.equalsIgnoreCase("subclass")
                || tagName.equalsIgnoreCase("joined-subclass")
                || tagName.equalsIgnoreCase("union-subclass")) {
            this.updateClassAttribute(node, "name");
            this.updateClassAttribute(node, "extends");
            this.updateClassAttribute(node, "entity-name");
            this.updateClassAttribute(node, "persister");
            if (!continueAfterMatch) {
                return;
            }
        }

        Enumeration attributeNames = node.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String attributeName = (String) attributeNames.nextElement();
            ObservableHolder attribute = node.getAttributeValue(attributeName);
            attribute.setValue(this.updateReferenceValue((String) attribute.getValue(), attributeName, false));
        }
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public HibernateMappingHandler(
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

    public void updatePropertyAttribute(XmlElementNode xmlElementNode, ProgramClass programClass1) throws ZkmException, IOException {
        ObservableHolder observableHolder = xmlElementNode.getAttributeValue("name");
        if (observableHolder != null) {
            String tagNameValue = xmlElementNode.getTagName();
            String value = (String) observableHolder.getValue();
            if (programClass1 != null) {
                this.updatePropertyName(tagNameValue, value, observableHolder, programClass1);
            }
        }
    }

    private static void staticInit() {
        builtInGeneratorNames = ZkmUtils.createHashSet();
        builtInGeneratorNames.add("increment");
        builtInGeneratorNames.add("identity");
        builtInGeneratorNames.add("sequence");
        builtInGeneratorNames.add("hilo");
        builtInGeneratorNames.add("seqhilo");
        builtInGeneratorNames.add("uuid");
        builtInGeneratorNames.add("guid");
        builtInGeneratorNames.add("native");
        builtInGeneratorNames.add("assigned");
        builtInGeneratorNames.add("select");
        builtInGeneratorNames.add("foreign");
        builtInGeneratorNames.add("sequence-identity");
    }
}
