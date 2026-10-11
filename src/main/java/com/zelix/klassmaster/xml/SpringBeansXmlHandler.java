package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ListMultimap;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;
import com.zelix.klassmaster.util.SetMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.UniqueWorkQueue;
import com.zelix.klassmaster.util.ZkmAssert;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class SpringBeansXmlHandler extends SpringXmlHandlerBase {
    public ListMultimap childBeansByParent;

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        TwoKeyMap twoKeyMap = (TwoKeyMap) object2;
        Map map3 = (Map) object1;
        Map map2 = (Map) object;
        String tagName = xmlElementNode.getTagName();
        String openElementName = this.getOpenElementName(1);
        if (tagName.equalsIgnoreCase("bean") && openElementName != null && openElementName.equalsIgnoreCase("beans")) {
            boolean bl = false;
            boolean bl1 = false;
            String valueValue14 = "reference in '" + super.resourceName + "' by '" + tagName + "' tag";
            String dottedClassName = null;
            ObservableHolder observableHolder6 = xmlElementNode.getAttributeValue("class");
            List list1 = null;
            ObservableHolder observableHolder7 = xmlElementNode.getAttributeValue("id");
            if (observableHolder7 != null) {
                list1 = new ArrayList(1);
                list1.add(observableHolder7.getValue());
            } else {
                ObservableHolder observableHolder8 = xmlElementNode.getAttributeValue("name");
                if (observableHolder8 != null) {
                    list1 = this.splitBeanNames((String) observableHolder8.getValue());
                }
            }

            if (observableHolder6 != null) {
                String valueValue = (String) observableHolder6.getValue();
                ObservableHolder observableHolder3 = new ObservableHolder();
                dottedClassName = this.addClassReference(valueValue, map1, observableHolder3, valueValue14 + " '" + "class" + "' attribute");
                if (!observableHolder3.isValueNull() && list1 != null) {
                    Iterator iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        ProgramClass programClass7 = (ProgramClass) super.beanClasses.put(key, observableHolder3.getValue());
                    }
                }
            }

            if (dottedClassName == null) {
                ObservableHolder observableHolder9 = xmlElementNode.getAttributeValue("factory-bean");
                if (observableHolder9 != null) {
                    String valueValue2 = (String) observableHolder9.getValue();
                    ProgramClass programClass4 = (ProgramClass) super.beanClasses.get(valueValue2);
                    if (programClass4 != null && list1 != null) {
                        dottedClassName = programClass4.getDottedClassName();
                        Iterator iterator4 = list1.iterator();

                        while (iterator4.hasNext()) {
                            String keyValue = (String) iterator4.next();
                            super.beanClasses.put(keyValue, programClass4);
                        }
                    }
                }
            }

            if (dottedClassName == null) {
                ObservableHolder observableHolder10 = xmlElementNode.getAttributeValue("parent");
                if (observableHolder10 != null && list1 != null) {
                    bl1 = true;
                    String valueValue3 = (String) observableHolder10.getValue();
                    Iterator iterator2 = list1.iterator();

                    while (iterator2.hasNext()) {
                        String keyValue2 = (String) iterator2.next();
                        super.beanParents.put(keyValue2, valueValue3);
                        String keyValue3 = valueValue3;
                        ProgramClass programClass2 = null;

                        while (keyValue3 != null && programClass2 == null) {
                            programClass2 = (ProgramClass) super.beanClasses.get(keyValue3);
                            if (programClass2 != null) {
                                dottedClassName = programClass2.getDottedClassName();
                                super.beanClasses.put(keyValue2, programClass2);
                            } else {
                                keyValue3 = (String) super.beanParents.get(keyValue3);
                            }
                        }
                    }
                }
            }

            if (dottedClassName == null) {
                ObservableHolder observableHolder11 = xmlElementNode.getAttributeValue("abstract");
                if (observableHolder11 != null && ((String) observableHolder11.getValue()).equalsIgnoreCase("true")) {
                    bl = true;
                    XmlElementNode xmlElementNode2 = this.getOpenElement(1);
                    if (list1 != null) {
                        Iterator iterator3 = list1.iterator();

                        while (iterator3.hasNext()) {
                            String keyValue4 = (String) iterator3.next();
                            AbstractXmlBeanRef abstractXmlBeanRef1 = new AbstractXmlBeanRef(xmlElementNode, xmlElementNode2);
                            super.abstractBeans.put(keyValue4, abstractXmlBeanRef1);
                        }
                    }
                }
            }

            if (dottedClassName == null) {
                if (!bl) {
                    if (HiddenOptionFlags.ASSERT_SPRING_BEAN_CLASS) {
                        Object[] objects2 = new Object[]{super.resourceName, xmlElementNode, 0};
                        ZkmAssert.assertNotNullArgs(dottedClassName, objects2);
                    } else {
                        super.messageReporter.reportError("ERROR:", "Could not identify bean class in : '" + super.resourceName + "' : '" + xmlElementNode + "' (A)");
                    }
                }

                return;
            }

            ProgramClass programClass3 = ClassHierarchyNode.findProgramClass(ZkmUtils.dotsToSlashes(dottedClassName));
            if (programClass3 != null) {
                DescriptorMatcher descriptorMatcher1 = WildcardStringMatcher.MATCH_ALL;
                String valueValue15 = valueValue14;
                TwoKeyMap twoKeyMap1 = twoKeyMap;
                Map map5 = map3;
                Map map4 = map1;
                Integer integer = 0;
                Object object4 = null;
                DescriptorMatcher descriptorMatcher = descriptorMatcher1;
                Object valueValue17 = null;
                this.addMatchingMethodReferences("<init>", (String) null, valueValue17, descriptorMatcher, (String) object4, integer, map4, map5, twoKeyMap1, valueValue15);
            }

            boolean bl2 = false;
            boolean bl3 = false;
            Enumeration enumeration1 = xmlElementNode.getAttributeNames();

            while (enumeration1.hasMoreElements()) {
                String keyValue8 = (String) enumeration1.nextElement();
                ObservableHolder observableHolder12 = xmlElementNode.getAttributeValue(keyValue8);
                String valueValue4 = (String) observableHolder12.getValue();
                String valueValue16 = valueValue14 + " '" + keyValue8 + "' attribute";
                if (!keyValue8.equalsIgnoreCase("class")) {
                    if (keyValue8.equalsIgnoreCase("init-method")) {
                        MethodSignature methodSignature4 = new MethodSignature(valueValue4, "()V");
                        Object[] objects1 = new Object[]{methodSignature4, super.resourceName, xmlElementNode, 1};
                        ZkmAssert.assertNotNullArgs(dottedClassName, objects1);
                        this.addMethodReference(dottedClassName, methodSignature4, map3, valueValue16);
                        bl2 = true;
                    } else if (keyValue8.equalsIgnoreCase("destroy-method")) {
                        MethodSignature methodSignature3 = new MethodSignature(valueValue4, "()V");
                        Object[] objects = new Object[]{methodSignature3, super.resourceName, xmlElementNode, 2};
                        ZkmAssert.assertNotNullArgs(dottedClassName, objects);
                        this.addMethodReference(dottedClassName, methodSignature3, map3, valueValue16);
                        bl3 = true;
                    } else if (keyValue8.equalsIgnoreCase("factory-method")) {
                        if (programClass3 != null) {
                            this.addHierarchyMethodReferences(
                                    programClass3, valueValue4, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 8, map3, twoKeyMap, valueValue14
                            );
                        }
                    } else if (keyValue8.equalsIgnoreCase("parent")) {
                        bl1 = true;
                        if (list1 != null) {
                            Iterator iterator1 = list1.iterator();

                            while (iterator1.hasNext()) {
                                String keyValue5 = (String) iterator1.next();
                                super.beanParents.put(keyValue5, valueValue4);
                            }
                        }
                    }
                }
            }

            if (bl1) {
                String keyValue6 = null;
                Map map6;
                if (list1 != null) {
                    Iterator iterator5 = list1.iterator();

                    while (true) {
                        if (!iterator5.hasNext()) {
                            map6 = super.abstractBeans;
                            break;
                        }

                        String keyValue7 = (String) iterator5.next();
                        keyValue6 = (String) super.beanParents.get(keyValue7);
                        if (keyValue6 != null) {
                            map6 = super.abstractBeans;
                            break;
                        }
                    }
                } else {
                    map6 = super.abstractBeans;
                }

                AbstractXmlBeanRef abstractXmlBeanRef2 = (AbstractXmlBeanRef) map6.get(keyValue6);

                while (keyValue6 != null && abstractXmlBeanRef2 != null) {
                    XmlElementNode xmlElementNode4 = abstractXmlBeanRef2.getBeanElement();
                    if (!bl2
                            && xmlElementNode4.getAttributeValue("default-init-method") != null
                            && xmlElementNode4.getAttributeValue("default-init-method").getValue() != null) {
                        String valueValue5 = (String) xmlElementNode4.getAttributeValue("default-init-method").getValue();
                        MethodSignature methodSignature5 = new MethodSignature(valueValue5, "()V");
                        this.addMethodReference(
                                dottedClassName,
                                methodSignature5,
                                map3,
                                "reference in '" + super.resourceName + "' by '" + "bean" + "' tag " + "default-init-method" + "='" + valueValue5 + "'"
                        );
                        bl2 = true;
                    }

                    if (!bl3
                            && xmlElementNode4.getAttributeValue("default-destroy-method") != null
                            && xmlElementNode4.getAttributeValue("default-destroy-method").getValue() != null) {
                        String valueValue6 = (String) xmlElementNode4.getAttributeValue("default-destroy-method").getValue();
                        MethodSignature methodSignature6 = new MethodSignature(valueValue6, "()V");
                        this.addMethodReference(
                                dottedClassName,
                                methodSignature6,
                                map3,
                                "reference in '" + super.resourceName + "' by '" + "bean" + "' tag " + "default-destroy-method" + "='" + valueValue6 + "'"
                        );
                        bl3 = true;
                    }

                    XmlElementNode xmlElementNode5 = abstractXmlBeanRef2.getParentElement();
                    if (!bl2
                            && xmlElementNode5.getAttributeValue("default-init-method") != null
                            && xmlElementNode5.getAttributeValue("default-init-method").getValue() != null) {
                        String valueValue7 = (String) xmlElementNode5.getAttributeValue("default-init-method").getValue();
                        MethodSignature methodSignature7 = new MethodSignature(valueValue7, "()V");
                        this.addMethodReference(
                                dottedClassName,
                                methodSignature7,
                                map3,
                                "reference in '" + super.resourceName + "' by '" + "beans" + "' tag " + "default-init-method" + "='" + valueValue7 + "'"
                        );
                        if (programClass3 != null) {
                            super.resourceFileIndex.addDefaultMethodClass(super.resourceName, "default-init-method", programClass3);
                        }

                        bl2 = true;
                    }

                    if (!bl3
                            && xmlElementNode5.getAttributeValue("default-destroy-method") != null
                            && xmlElementNode5.getAttributeValue("default-destroy-method").getValue() != null) {
                        String valueValue8 = (String) xmlElementNode5.getAttributeValue("default-destroy-method").getValue();
                        MethodSignature methodSignature8 = new MethodSignature(valueValue8, "()V");
                        this.addMethodReference(
                                dottedClassName,
                                methodSignature8,
                                map3,
                                "reference in '" + super.resourceName + "' by '" + "beans" + "' tag " + "default-destroy-method" + "='" + valueValue8 + "'"
                        );
                        if (programClass3 != null) {
                            super.resourceFileIndex.addDefaultMethodClass(super.resourceName, "default-destroy-method", programClass3);
                        }

                        bl3 = true;
                    }

                    List list2 = abstractXmlBeanRef2.getPropertyElements();
                    Iterator iterator6 = list2.iterator();

                    while (iterator6.hasNext()) {
                        ObservableHolder observableHolder4 = ((XmlElementNode) iterator6.next()).getAttributeValue("name");
                        if (observableHolder4 != null) {
                            String valueValue9 = (String) observableHolder4.getValue();
                            String valueValue18 = "reference in '" + super.resourceName + "' by '" + tagName + "' tag '" + "name" + "' attribute (B)";
                            if (programClass3 != null) {
                                this.addPropertyAccessorReferences(programClass3, valueValue9, map3, twoKeyMap, valueValue18);
                            }
                        }
                    }

                    keyValue6 = (String) super.beanParents.get(keyValue6);
                    if (keyValue6 != null) {
                        abstractXmlBeanRef2 = (AbstractXmlBeanRef) super.abstractBeans.get(keyValue6);
                    }
                }
            }

            XmlElementNode xmlElementNode3 = this.getOpenElement(1);
            if (!bl2
                    && xmlElementNode3.getAttributeValue("default-init-method") != null
                    && xmlElementNode3.getAttributeValue("default-init-method").getValue() != null) {
                String valueValue10 = (String) xmlElementNode3.getAttributeValue("default-init-method").getValue();
                MethodSignature methodSignature1 = new MethodSignature(valueValue10, "()V");
                this.addMethodReference(
                        dottedClassName,
                        methodSignature1,
                        map3,
                        "reference in '" + super.resourceName + "' by '" + "beans" + "' tag " + "default-init-method" + "='" + valueValue10 + "'"
                );
                if (programClass3 != null) {
                    super.resourceFileIndex.addDefaultMethodClass(super.resourceName, "default-init-method", programClass3);
                }
            }

            if (!bl3
                    && xmlElementNode3.getAttributeValue("default-destroy-method") != null
                    && xmlElementNode3.getAttributeValue("default-destroy-method").getValue() != null) {
                String valueValue11 = (String) xmlElementNode3.getAttributeValue("default-destroy-method").getValue();
                MethodSignature methodSignature2 = new MethodSignature(valueValue11, "()V");
                this.addMethodReference(
                        dottedClassName,
                        methodSignature2,
                        map3,
                        "reference in '" + super.resourceName + "' by '" + "beans" + "' tag " + "default-destroy-method" + "='" + valueValue11 + "'"
                );
                if (programClass3 != null) {
                    super.resourceFileIndex.addDefaultMethodClass(super.resourceName, "default-destroy-method", programClass3);
                }
            }
        } else if (tagName.equalsIgnoreCase("import") && openElementName != null && openElementName.equalsIgnoreCase("beans")) {
            Enumeration enumeration = xmlElementNode.getAttributeNames();

            while (enumeration.hasMoreElements()) {
                String keyValue9 = (String) enumeration.nextElement();
                if (keyValue9.equalsIgnoreCase("resource")) {
                    ObservableHolder observableHolder5 = xmlElementNode.getAttributeValue(keyValue9);
                    String valueValue12 = (String) observableHolder5.getValue();
                    this.processImportResource(valueValue12);
                }
            }
        } else if (tagName.equalsIgnoreCase("property") && openElementName != null && openElementName.equalsIgnoreCase("bean")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("name");
            if (observableHolder != null) {
                String valueValue13 = (String) observableHolder.getValue();
                String valueValue19 = "reference in '" + super.resourceName + "' by '" + tagName + "' tag '" + "name" + "' attribute (A)";
                String string4 = this.findEnclosingBeanClassName();
                if (string4 != null) {
                    ProgramClass programClass1 = this.findProgramClass(string4);
                    if (programClass1 != null) {
                        this.addPropertyAccessorReferences(programClass1, valueValue13, map3, twoKeyMap, valueValue19);
                    }
                } else {
                    XmlElementNode xmlElementNode1 = this.getOpenElement(1);
                    ObservableHolder observableHolder1 = xmlElementNode1.getAttributeValue("abstract");
                    if (observableHolder1 != null && ((String) observableHolder1.getValue()).equalsIgnoreCase("true")) {
                        ObservableHolder observableHolder2 = xmlElementNode1.getAttributeValue("id");
                        if (observableHolder2 != null) {
                            AbstractXmlBeanRef abstractXmlBeanRef = (AbstractXmlBeanRef) super.abstractBeans.get(observableHolder2.getValue());
                            if (abstractXmlBeanRef != null) {
                                abstractXmlBeanRef.addPropertyElement(xmlElementNode);
                            }
                        }
                    }
                }
            }
        } else {
            super.analyzeElement(xmlElementNode, map1, map2, map3, twoKeyMap);
        }
    }

    public ProgramClass findChildBeanClass(Object keyArgument, ListMultimap listMultimap) {
        List list1 = listMultimap.getValues(keyArgument);
        ProgramClass programClass1 = null;
        if (list1 != null) {
            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                programClass1 = (ProgramClass) super.beanClasses.get(key);
                if (programClass1 != null) {
                    break;
                }

                programClass1 = this.findChildBeanClass(key, listMultimap);
                if (programClass1 != null) {
                    break;
                }
            }
        }

        return programClass1;
    }

    public String findEnclosingBeanClassName() {
        for (int i = 0; i < super.elementStack.size(); i++) {
            XmlElementNode xmlElementNode = (XmlElementNode) super.elementStack.peekAt(i);
            String tagName = xmlElementNode.getTagName();
            XmlElementNode xmlElementNode1;
            String keyValue;
            if (!tagName.equals("beans")) {
                if (!tagName.equals("bean")) {
                    continue;
                }

                xmlElementNode1 = xmlElementNode;
                long bj = 24856730383790L;
                keyValue = "id";
            } else {
                xmlElementNode1 = xmlElementNode;
                long bb = 24856730383790L;
                keyValue = "id";
            }

            String keyValue2 = keyValue;
            ObservableHolder observableHolder = xmlElementNode1.getAttributeValue(keyValue2);
            List list1 = null;
            if (observableHolder != null) {
                ProgramClass programClass1 = (ProgramClass) super.beanClasses.get(observableHolder.getValue());
                if (programClass1 != null) {
                    return programClass1.getDottedClassName();
                }
            } else {
                ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("name");
                if (observableHolder1 != null) {
                    list1 = this.splitBeanNames((String) observableHolder1.getValue());
                    Iterator iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();
                        ProgramClass programClass2 = (ProgramClass) super.beanClasses.get(key);
                        if (programClass2 != null) {
                            return programClass2.getDottedClassName();
                        }
                    }
                }
            }

            ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue("class");
            if (observableHolder2 != null) {
                return (String) observableHolder2.getValue();
            }

            ObservableHolder observableHolder3 = xmlElementNode.getAttributeValue("factory-bean");
            if (observableHolder3 != null) {
                String valueValue = (String) observableHolder3.getValue();
                ProgramClass programClass4 = (ProgramClass) super.beanClasses.get(valueValue);
                if (programClass4 != null) {
                    return programClass4.getDottedClassName();
                }

                xmlElementNode1 = xmlElementNode;
                long bc = 24856730383790L;
                keyValue = "abstract";
            } else {
                xmlElementNode1 = xmlElementNode;
                long bd = 24856730383790L;
                keyValue = "abstract";
            }

            ObservableHolder observableHolder4 = xmlElementNode1.getAttributeValue(keyValue);
            if ((observableHolder != null || list1 != null && list1.size() > 0) && observableHolder4 != null) {
                if (((String) observableHolder4.getValue()).equalsIgnoreCase("true")) {
                    if (this.childBeansByParent == null) {
                        this.childBeansByParent = new ListMultimap();
                        Iterator iterator1 = super.beanParents.entrySet().iterator();

                        while (iterator1.hasNext()) {
                            Entry entry = (Entry) iterator1.next();
                            this.childBeansByParent.addValue(entry.getValue(), entry.getKey());
                        }
                    }

                    ArrayList arrayList = new ArrayList();
                    if (observableHolder != null) {
                        String valueValue2 = (String) observableHolder.getValue();
                        arrayList.add(valueValue2);
                    } else {
                        arrayList.addAll(list1);
                    }

                    Iterator iterator2 = arrayList.iterator();

                    while (iterator2.hasNext()) {
                        String keyValue4 = (String) iterator2.next();
                        ProgramClass programClass3 = this.findChildBeanClass(keyValue4, this.childBeansByParent);
                        if (programClass3 != null) {
                            return programClass3.getDottedClassName();
                        }
                    }

                }
            }
            xmlElementNode1 = xmlElementNode;
            keyValue = "parent";

            String keyValue3 = keyValue;
            ObservableHolder observableHolder5 = xmlElementNode1.getAttributeValue(keyValue3);
            if (observableHolder5 != null) {
                String valueValue3 = (String) observableHolder5.getValue();

                for (ProgramClass programClass5 = null; valueValue3 != null && programClass5 == null; valueValue3 = (String) super.beanParents.get(valueValue3)) {
                    programClass5 = (ProgramClass) super.beanClasses.get(valueValue3);
                    if (programClass5 != null) {
                        return programClass5.getDottedClassName();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void rewriteElement(Object object, Object object1) throws ZkmException, IOException {
        List list1 = (List) object1;
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        String tagName = xmlElementNode.getTagName();
        String openElementName = this.getOpenElementName(1);
        String string2 = this.findEnclosingBeanClassName();
        boolean predicateFlag = XmlConfigFileHandler.getPredicateFlag();
        String renamedClassName = null;
        boolean predicateFlagValue = predicateFlag;
        if (string2 != null) {
            renamedClassName = this.getRenamedClassName(string2);
        }

        if (tagName.equalsIgnoreCase("bean") && openElementName != null && openElementName.equalsIgnoreCase("beans")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("class");
            if (observableHolder != null) {
                String valueValue = (String) observableHolder.getValue();
                if (!valueValue.equals(renamedClassName)) {
                    observableHolder.setValue(renamedClassName);
                }
            }
        }

        if (tagName.equalsIgnoreCase("beans") && openElementName == null) {
            SetMultiMap setMultiMap = super.resourceFileIndex.getDefaultMethodClasses(super.resourceName);
            if (setMultiMap != null) {
                Object[] objects = new Object[0];
                Iterator iterator = setMultiMap.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    String key = (String) entry.getKey();
                    ObservableHolder observableHolder9 = xmlElementNode.getAttributeValue(key);
                    if (observableHolder9 != null) {
                        String valueValue2 = (String) observableHolder9.getValue();
                        MethodSignature methodSignature3 = new MethodSignature(valueValue2, "()V");
                        Set set1 = (Set) entry.getValue();
                        StringBuilder stringBuilder = new StringBuilder();
                        String valueValue3 = null;
                        BooleanFlag booleanFlag1 = new BooleanFlag();
                        Iterator iterator1 = set1.iterator();

                        while (iterator1.hasNext()) {
                            ProgramClass programClass1 = (ProgramClass) iterator1.next();
                            String renamedMethodName = this.getRenamedMethodName(programClass1.getDottedClassName(), methodSignature3, booleanFlag1);
                            if (stringBuilder.length() > 0) {
                                stringBuilder.append(",");
                            }

                            stringBuilder.append(programClass1.getDottedClassName());
                            if (valueValue3 == null) {
                                if (booleanFlag1.getValue()) {
                                    valueValue3 = renamedMethodName;
                                }
                            } else if (booleanFlag1.getValue() && !renamedMethodName.equals(valueValue3)) {
                                super.messageReporter
                                        .reportError(
                                                "ERROR:",
                                                "Clashing new method names for attribute '"
                                                        + key
                                                        + "' of tag '"
                                                        + "beans"
                                                        + "' in  XML file '"
                                                        + super.resourceName
                                                        + "' : '"
                                                        + valueValue3
                                                        + "' '"
                                                        + renamedMethodName
                                                        + "' : '"
                                                        + stringBuilder
                                                        + "'"
                                        );
                            }

                            if (predicateFlagValue) {
                                break;
                            }
                        }

                        if (!valueValue2.equals(valueValue3)) {
                            observableHolder9.setValue(valueValue3);
                        }
                    }

                    if (predicateFlagValue) {
                        break;
                    }
                }
            }
        } else if (tagName.equalsIgnoreCase("bean") && openElementName != null && openElementName.equalsIgnoreCase("beans")) {
            if (renamedClassName == null) {
                list1.add(xmlElementNode);
                return;
            }

            Enumeration enumeration2 = xmlElementNode.getAttributeNames();

            while (enumeration2.hasMoreElements()) {
                String keyValue = (String) enumeration2.nextElement();
                ObservableHolder observableHolder6 = xmlElementNode.getAttributeValue(keyValue);
                String valueValue4 = (String) observableHolder6.getValue();
                if (!keyValue.equalsIgnoreCase("class")) {
                    if (keyValue.equalsIgnoreCase("init-method")) {
                        MethodSignature methodSignature1 = new MethodSignature(valueValue4, "()V");
                        String renamedMethodNameValue = this.getRenamedMethodName(renamedClassName, methodSignature1, new BooleanFlag());
                        if (!valueValue4.equals(renamedMethodNameValue)) {
                            observableHolder6.setValue(renamedMethodNameValue);
                        }
                    } else if (keyValue.equalsIgnoreCase("destroy-method")) {
                        MethodSignature methodSignature2 = new MethodSignature(valueValue4, "()V");
                        String renamedMethodNameValue2 = this.getRenamedMethodName(renamedClassName, methodSignature2, new BooleanFlag());
                        if (!valueValue4.equals(renamedMethodNameValue2)) {
                            observableHolder6.setValue(renamedMethodNameValue2);
                        }
                    } else if (keyValue.equalsIgnoreCase("factory-method")) {
                        ObservableHolder observableHolder8 = new ObservableHolder();
                        BooleanFlag booleanFlag = new BooleanFlag();
                        String valueValue5 = this.resolveRenamedMethodName(renamedClassName, valueValue4, 8, observableHolder8, booleanFlag);
                        if (valueValue5 != null) {
                            if (!valueValue4.equals(valueValue5)) {
                                observableHolder6.setValue(valueValue5);
                            }
                        } else if (booleanFlag.getValue()) {
                            super.messageReporter
                                    .reportError(
                                            "ERROR:",
                                            "Could not update '"
                                                    + keyValue
                                                    + "' attribute of tag '"
                                                    + tagName
                                                    + "' in  XML file '"
                                                    + super.resourceName
                                                    + "' : existing value is '"
                                                    + valueValue4
                                                    + "' : \""
                                                    + (String) observableHolder8.getValue()
                                                    + "\""
                                    );
                        }
                    }
                }

                if (predicateFlagValue) {
                    break;
                }
            }
        } else if (tagName.equalsIgnoreCase("import") && openElementName != null && openElementName.equalsIgnoreCase("beans")) {
            Enumeration enumeration1 = xmlElementNode.getAttributeNames();

            while (enumeration1.hasMoreElements()) {
                String keyValue2 = (String) enumeration1.nextElement();
                ObservableHolder observableHolder5 = xmlElementNode.getAttributeValue(keyValue2);
                String valueValue6 = (String) observableHolder5.getValue();
                if (keyValue2.equalsIgnoreCase("resource")) {
                    if (valueValue6.startsWith("classpath:") || valueValue6.startsWith("classpath*:") || valueValue6.startsWith("file:")) {
                        int ba = valueValue6.indexOf(":");
                        String string6 = valueValue6.substring(0, ba + 1);
                        String valueValue14 = valueValue6.substring(ba + 1);
                        if (valueValue14.startsWith("/")) {
                            string6 = string6 + "/";
                            valueValue14 = valueValue14.substring(1);
                        }

                        ObservableHolder observableHolder2 = new ObservableHolder();
                        ObservableHolder observableHolder3 = new ObservableHolder();
                        String string8 = ZkmFileUtils.splitPath(valueValue14, observableHolder2, observableHolder3);
                        String valueValue7 = (String) observableHolder2.getValue();
                        if (valueValue7.length() > 0) {
                            String valueValue8 = (String) observableHolder3.getValue();
                            String string11 = this.renamePackagePath(valueValue7);
                            if (!string11.equals(valueValue7)) {
                                String valueValue9 = string6 + string11 + string8 + valueValue8;
                                observableHolder5.setValue(valueValue9);
                            }
                        }

                    }

                    if (predicateFlagValue || !(valueValue6.startsWith("classpath:") || valueValue6.startsWith("classpath*:") || valueValue6.startsWith("file:"))) {
                        ObservableHolder observableHolder7 = new ObservableHolder();
                        ObservableHolder observableHolder10 = new ObservableHolder();
                        ZkmFileUtils.splitPath(super.entryName, observableHolder7, observableHolder10);
                        String valueValue10 = (String) observableHolder7.getValue();
                        String string24 = "";
                        String valueValue15 = valueValue6;
                        if (valueValue15.startsWith("/")) {
                            new StringBuilder().append(string24).append("/").toString();
                            valueValue15.substring(1);
                        }

                        String valueValue11 = (String) observableHolder7.getValue();
                        if (valueValue11.length() > 0) {
                            String valueValue12 = this.translateRelativePath(valueValue10, valueValue6, true);
                            if (!valueValue12.equals(valueValue6)) {
                                observableHolder5.setValue(valueValue12);
                            }
                        }
                    }
                }

                if (predicateFlagValue) {
                    break;
                }
            }
        } else if (tagName.equalsIgnoreCase("property") && openElementName != null && openElementName.equalsIgnoreCase("bean")) {
            ObservableHolder observableHolder4 = xmlElementNode.getAttributeValue("name");
            if (observableHolder4 != null) {
                String valueValue13 = (String) observableHolder4.getValue();
                if (renamedClassName != null) {
                    this.resolveRenamedPropertyName(renamedClassName, string2, valueValue13, observableHolder4);
                }
            }
        } else {
            Enumeration enumeration = xmlElementNode.getAttributeNames();

            while (enumeration.hasMoreElements()) {
                String keyValue3 = (String) enumeration.nextElement();
                ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue(keyValue3);
                observableHolder1.setValue(this.updateReferenceValue((String) observableHolder1.getValue(), keyValue3, false));
            }
        }

        list1.add(xmlElementNode);
    }

    @Override
    public List resolveImportedResources(String string, String string1) throws ZkmException, IOException {
        ArrayList arrayList = new ArrayList();
        Iterator iterator = super.resourceFileIndex.loadMatchingConfigFiles(string, string1, XmlConfigFileType.SPRING, super.messageReporter).iterator();

        while (iterator.hasNext()) {
            TextResourceContent textResourceContent = (TextResourceContent) iterator.next();

            try {
                HashSet hashSet = ZkmUtils.createHashSet();
                SpringImportedBeansXmlHandler springImportedBeansXmlHandler = new SpringImportedBeansXmlHandler(
                        textResourceContent.resourceName,
                        super.resourceFileIndex,
                        hashSet,
                        super.beanClasses,
                        super.abstractBeans,
                        super.beanParents,
                        super.classMemberLookup,
                        super.classResolver,
                        super.messageReporter
                );
                new XmlResourceReader(
                        textResourceContent.resourceName,
                        textResourceContent.content,
                        textResourceContent.skipByteCount,
                        textResourceContent.bomLength,
                        textResourceContent.byteOrder,
                        (String) textResourceContent.encoding.getValue(),
                        springImportedBeansXmlHandler
                );
                arrayList.add(textResourceContent.resourceName);
                Map map1 = springImportedBeansXmlHandler.copyBeanClasses();
                Iterator iterator1 = map1.entrySet().iterator();

                while (iterator1.hasNext()) {
                    Entry entry = (Entry) iterator1.next();
                    ProgramClass programClass1 = (ProgramClass) super.beanClasses.put(entry.getKey(), entry.getValue());
                }

                Map map2 = springImportedBeansXmlHandler.copyAbstractBeans();
                Iterator iterator2 = map2.entrySet().iterator();

                while (iterator2.hasNext()) {
                    Entry entry1 = (Entry) iterator2.next();
                    AbstractXmlBeanRef abstractXmlBeanRef = (AbstractXmlBeanRef) super.abstractBeans.put(entry1.getKey(), entry1.getValue());
                }

                Map map3 = springImportedBeansXmlHandler.copyBeanParents();
                Iterator iterator3 = map3.entrySet().iterator();

                while (iterator3.hasNext()) {
                    Entry entry2 = (Entry) iterator3.next();
                    String string2 = (String) super.beanParents.put(entry2.getKey(), entry2.getValue());
                }

                super.resourceFileIndex.addBeanClasses(super.resourceName, map1);
                super.resourceFileIndex.addBeanParents(super.resourceName, map3);
            } catch (ZkmException zkmException) {
                super.messageReporter.reportError("FILE ERROR:", "Invalid XML file '" + textResourceContent.resourceName + "' (C) : " + zkmException.getMessage());
            } catch (IOException iOException) {
                super.messageReporter
                        .reportError("FILE ERROR:", "Error reading XML file '" + textResourceContent.resourceName + "' (C) : " + iOException.getMessage());
            } catch (Exception exception) {
                super.messageReporter
                        .reportError("FILE ERROR:", "Unexpected error analyzing XML file '" + super.resourceName + "' (C) : " + exception.getMessage());
            }
        }

        return arrayList;
    }

    public SpringBeansXmlHandler(
            String key, ResourceFileIndex resourceFileIndex1, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1
    ) {
        super(key, resourceFileIndex1, classMemberLookup1, classResolver1, messageReporter1);
        Map map1 = resourceFileIndex1.getBeanClasses(key);
        if (map1 != null) {
            super.beanClasses.putAll(map1);
        }

        Map map2 = resourceFileIndex1.getBeanParents(key);
        if (map2 != null) {
            super.beanParents.putAll(map2);
        }
    }

    @Override
    public void finishAnalysis(Object object, Object object1) throws ZkmException, IOException {
        super.resourceFileIndex.addBeanParents(super.resourceName, super.beanParents);
        super.resourceFileIndex.addBeanClasses(super.resourceName, super.beanClasses);
        List list1 = super.resourceFileIndex.getImportedFiles(super.resourceName);
        if (list1 != null) {
            HashSet hashSet = ZkmUtils.createHashSet();
            UniqueWorkQueue uniqueWorkQueue = new UniqueWorkQueue();
            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                String bufferElement = (String) iterator.next();
                uniqueWorkQueue.enqueue(bufferElement);
                hashSet.add(bufferElement);
            }

            while (!uniqueWorkQueue.isEmpty()) {
                String key = (String) uniqueWorkQueue.dequeue();
                List list3 = super.resourceFileIndex.getImportingFiles(key);
                if (list3 != null) {
                    Iterator iterator1 = list3.iterator();

                    while (iterator1.hasNext()) {
                        String keyValue = (String) iterator1.next();
                        Map map1 = super.resourceFileIndex.getBeanParents(keyValue);
                        ResourceFileIndex resourceFileIndex1;
                        if (map1 != null) {
                            super.resourceFileIndex.addBeanParents(key, map1);
                            resourceFileIndex1 = super.resourceFileIndex;
                        } else {
                            resourceFileIndex1 = super.resourceFileIndex;
                        }

                        Map map2 = resourceFileIndex1.getBeanClasses(keyValue);
                        if (map2 != null) {
                            super.resourceFileIndex.addBeanClasses(key, map2);
                            resourceFileIndex1 = super.resourceFileIndex;
                        } else {
                            resourceFileIndex1 = super.resourceFileIndex;
                        }

                        List list2 = resourceFileIndex1.getImportedFiles(key);
                        if (list2 != null) {
                            Iterator iterator2 = list2.iterator();

                            while (iterator2.hasNext()) {
                                String bufferElementValue = (String) iterator2.next();
                                if (hashSet.add(bufferElementValue)) {
                                    uniqueWorkQueue.enqueue(bufferElementValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public SpringBeansXmlHandler(
            String key,
            EnumerableMap enumerableMap,
            ReadOnlyMultiMap readOnlyMultiMap,
            ReadOnlyMultiMap readOnlyMultiMap1,
            ResourcePathTranslator resourcePathTranslator1,
            ResourceFileIndex resourceFileIndex1,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            MessageReporter messageReporter1
    ) {
        super(
                key,
                enumerableMap,
                readOnlyMultiMap,
                readOnlyMultiMap1,
                resourceFileIndex1,
                resourcePathTranslator1,
                classMemberLookup1,
                classResolver1,
                messageReporter1
        );
        Map map1 = resourceFileIndex1.getBeanClasses(key);
        if (map1 != null) {
            super.beanClasses.putAll(map1);
        }

        Map map2 = resourceFileIndex1.getBeanParents(key);
        if (map2 != null) {
            super.beanParents.putAll(map2);
        }
    }
}
