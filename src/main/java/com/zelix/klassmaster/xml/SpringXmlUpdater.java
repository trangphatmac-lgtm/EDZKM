package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.classfile.MethodSignature;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.classfile.hierarchy.ClassHierarchyNode;
import com.zelix.klassmaster.classfile.hierarchy.ClassMemberLookup;
import com.zelix.klassmaster.classfile.hierarchy.ClassResolver;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.log.MessageReporter;
import com.zelix.klassmaster.util.BooleanFlag;
import com.zelix.klassmaster.util.EnumerableMap;
import com.zelix.klassmaster.util.ObjectStack;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ReadOnlyMultiMap;
import com.zelix.klassmaster.util.TwoKeyMap;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SpringXmlUpdater extends BeanPropertyXmlHandler {
    public final Map beanClasses = ZkmUtils.createHashMap();
    public final Map factoryMethodReferences = ZkmUtils.createHashMap();

    public SpringXmlUpdater(
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

    public SpringXmlUpdater(String string, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1) {
        super(string, classMemberLookup1, classResolver1, messageReporter1);
    }

    @Override
    public void beforeFlushOutput() throws ZkmException, IOException {
        boolean bl1 = XmlConfigFileHandler.alwaysTrue();
        Iterator iterator = this.factoryMethodReferences.entrySet().iterator();
        boolean bl = bl1;

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            String key = (String) entry.getKey();
            XmlMethodReference xmlMethodReference = (XmlMethodReference) entry.getValue();
            String beanNameOf = XmlMethodReference.getBeanNameOf(xmlMethodReference);
            ObservableHolder observableHolder = XmlMethodReference.getMethodNameHolderOf(xmlMethodReference);
            String referenceDescriptionOf = XmlMethodReference.getReferenceDescriptionOf(xmlMethodReference);
            ProgramClass programClass1 = (ProgramClass) this.beanClasses.get(beanNameOf);
            if (programClass1 != null) {
                ObservableHolder observableHolder1 = new ObservableHolder();
                BooleanFlag booleanFlag = new BooleanFlag();
                String valueValue = this.resolveRenamedMethodName(programClass1.getDottedClassName(), key, 0, observableHolder1, booleanFlag);
                if (valueValue != null) {
                    if (!key.equals(valueValue)) {
                        observableHolder.setValue(valueValue);
                    }
                } else if (booleanFlag.getValue()) {
                    super.messageReporter
                            .reportError(
                                    "ERROR:",
                                    "Could not update method name \""
                                            + key
                                            + "\" in  XML file \""
                                            + super.resourceName
                                            + "\" : \""
                                            + referenceDescriptionOf
                                            + "\" : \""
                                            + (String) observableHolder1.getValue()
                                            + "\""
                            );
                }
            }

            if (!bl) {
                break;
            }
        }
    }

    @Override
    public void finishAnalysis(Object object, Object object1) throws ZkmException, IOException {
        Map map1 = (Map) object;
        TwoKeyMap twoKeyMap = (TwoKeyMap) object1;
        Iterator iterator = this.factoryMethodReferences.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry entry = (Entry) iterator.next();
            String key = (String) entry.getKey();
            XmlMethodReference xmlMethodReference = (XmlMethodReference) entry.getValue();
            String beanNameOf = XmlMethodReference.getBeanNameOf(xmlMethodReference);
            String referenceDescriptionOf = XmlMethodReference.getReferenceDescriptionOf(xmlMethodReference);
            ProgramClass programClass1 = (ProgramClass) this.beanClasses.get(beanNameOf);
            if (programClass1 != null) {
                this.addHierarchyMethodReferences(
                        programClass1, key, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map1, twoKeyMap, referenceDescriptionOf
                );
            }
        }
    }

    @Override
    public void rewriteElement(Object object, Object object1) throws ZkmException, IOException {
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        boolean bl1 = XmlConfigFileHandler.alwaysTrue();
        String tagNameValue = xmlElementNode.getTagName();
        String openElementName = this.getOpenElementName(1);
        boolean skipResolvedReferencesValue = bl1;
        ProgramClass programClass1 = null;
        String string2 = null;
        if (tagNameValue.equalsIgnoreCase("bean")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("class");
            if (observableHolder != null) {
                String valueValue = (String) observableHolder.getValue();
                string2 = this.getRenamedClassName(valueValue);
                if (!valueValue.equals(string2)) {
                    observableHolder.setValue(string2);
                }

                if (string2 != null) {
                    programClass1 = ClassHierarchyNode.findProgramClass(ZkmUtils.dotsToSlashes(string2));
                }
            }

            ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("id");
            if (observableHolder1 != null) {
                String valueValue2 = (String) observableHolder1.getValue();
                if (programClass1 != null) {
                    this.beanClasses.put(valueValue2, programClass1);
                    string2 = programClass1.getDottedClassName();
                }
            }

            String valueValue3 = null;
            ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue("factory-ref");
            if (observableHolder2 != null) {
                valueValue3 = (String) observableHolder2.getValue();
                if (programClass1 == null) {
                    programClass1 = (ProgramClass) this.beanClasses.get(valueValue3);
                    if (programClass1 != null) {
                        string2 = programClass1.getDottedClassName();
                    }
                }
            }

            Enumeration enumeration = xmlElementNode.getAttributeNames();

            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                ObservableHolder observableHolder3 = xmlElementNode.getAttributeValue(key);
                String valueValue4 = (String) observableHolder3.getValue();
                if (!key.equalsIgnoreCase("class") && !key.equalsIgnoreCase("id") && !key.equalsIgnoreCase("factory-ref")) {
                    if (key.equalsIgnoreCase("init-method")) {
                        MethodSignature methodSignature1 = new MethodSignature(valueValue4, "()V");
                        String renamedMethodName = this.getRenamedMethodName(string2, methodSignature1, new BooleanFlag());
                        if (!valueValue4.equals(renamedMethodName)) {
                            observableHolder3.setValue(renamedMethodName);
                        }
                    } else if (key.equalsIgnoreCase("destroy-method")) {
                        MethodSignature methodSignature2 = new MethodSignature(valueValue4, "()V");
                        String renamedMethodNameValue = this.getRenamedMethodName(string2, methodSignature2, new BooleanFlag());
                        if (!valueValue4.equals(renamedMethodNameValue)) {
                            observableHolder3.setValue(renamedMethodNameValue);
                        }
                    } else {
                        if (key.equalsIgnoreCase("factory-method")) {
                            if (string2 != null) {
                                ObservableHolder observableHolder16 = new ObservableHolder();
                                BooleanFlag booleanFlag4 = new BooleanFlag();
                                String valueValue5 = this.resolveRenamedMethodName(string2, valueValue4, 0, observableHolder16, booleanFlag4);
                                if (valueValue5 != null) {
                                    if (!valueValue4.equals(valueValue5)) {
                                        observableHolder3.setValue(valueValue5);
                                    }
                                } else if (booleanFlag4.getValue()) {
                                    super.messageReporter
                                            .reportError(
                                                    "ERROR:",
                                                    "Could not update '"
                                                            + key
                                                            + "' attribute of tag '"
                                                            + tagNameValue
                                                            + "' in  XML file '"
                                                            + super.resourceName
                                                            + "' : existing value is '"
                                                            + valueValue4
                                                            + "' : \""
                                                            + (String) observableHolder16.getValue()
                                                            + "\""
                                            );
                                }

                            }

                            if ((string2 == null || !skipResolvedReferencesValue) && valueValue3 != null) {
                                String referenceDescriptionValue = "reference in '" + super.resourceName + "' by '" + "factory-method" + "' attribute in '" + tagNameValue + "' tag";
                                this.factoryMethodReferences.put(valueValue4, new XmlMethodReference(45314913602850L, valueValue3, observableHolder3, referenceDescriptionValue, null));
                            }
                        }
                    }
                }

                if (!skipResolvedReferencesValue) {
                    break;
                }
            }
        } else if (tagNameValue.equalsIgnoreCase("property") && openElementName != null && openElementName.equalsIgnoreCase("bean")) {
            ObservableHolder observableHolder6 = xmlElementNode.getAttributeValue("name");
            if (observableHolder6 != null) {
                String valueValue6 = (String) observableHolder6.getValue();
                String string16 = this.findEnclosingBeanClassName();
                if (string16 != null) {
                    string2 = this.getRenamedClassName(string16);
                    this.resolveRenamedPropertyName(string2, string16, valueValue6, observableHolder6);
                }
            }
        } else {
            boolean listenerTag = false;
            if (tagNameValue.equalsIgnoreCase("registration-listener")) {
                rewriteListenerMethods(xmlElementNode, tagNameValue, "registration-method", "unregistration-method", skipResolvedReferencesValue);
                listenerTag = true;
            }
            if (tagNameValue.equalsIgnoreCase("reference-listener")) {
                rewriteListenerMethods(xmlElementNode, tagNameValue, "bind-method", "unbind-method", skipResolvedReferencesValue);
                listenerTag = true;
            }
            if (!listenerTag || !skipResolvedReferencesValue) {
                Enumeration attributeNames = xmlElementNode.getAttributeNames();
                while (attributeNames.hasMoreElements()) {
                    String attributeName = (String) attributeNames.nextElement();
                    ObservableHolder attribute = xmlElementNode.getAttributeValue(attributeName);
                    attribute.setValue(this.updateReferenceValue((String) attribute.getValue(), attributeName, false));
                }
            }
        }

        ((List) object1).add(xmlElementNode);
    }

    private void rewriteListenerMethods(
            XmlElementNode node, String tagName, String firstAttributeName, String secondAttributeName, boolean skipResolvedReferences
    ) throws ZkmException, IOException {
        ObservableHolder beanReference = node.getAttributeValue("ref");
        if (beanReference == null) {
            return;
        }
        String beanName = (String) beanReference.getValue();
        ProgramClass beanClass = (ProgramClass) this.beanClasses.get(beanName);
        // The reference description uses the canonical XML tag spelling.
        String canonicalTagName = firstAttributeName.equals("registration-method") ? "registration-listener" : "reference-listener";
        String referenceDescription = "reference in '" + super.resourceName + "' by '" + canonicalTagName + "' tag";
        rewriteListenerMethod(node, tagName, firstAttributeName, beanName, beanClass, referenceDescription, skipResolvedReferences);
        rewriteListenerMethod(node, tagName, secondAttributeName, beanName, beanClass, referenceDescription, skipResolvedReferences);
    }

    private void rewriteListenerMethod(
            XmlElementNode node, String tagName, String attributeName, String beanName, ProgramClass beanClass,
            String referenceDescription, boolean skipResolvedReferences
    ) throws ZkmException, IOException {
        ObservableHolder attribute = node.getAttributeValue(attributeName);
        if (attribute == null) {
            return;
        }
        String methodName = (String) attribute.getValue();
        if (beanClass != null) {
            ObservableHolder resolutionError = new ObservableHolder();
            BooleanFlag resolutionFailed = new BooleanFlag();
            String renamedMethod = this.resolveRenamedMethodName(beanClass.getDottedClassName(), methodName, 0, resolutionError, resolutionFailed);
            if (renamedMethod != null) {
                if (!methodName.equals(renamedMethod)) {
                    attribute.setValue(renamedMethod);
                }
            } else if (resolutionFailed.getValue()) {
                super.messageReporter.reportError(
                        "ERROR:", "Could not update '" + attributeName + "' attribute of tag '" + tagName
                                + "' in  XML file '" + super.resourceName + "' : existing value is '" + methodName
                                + "' : \"" + (String) resolutionError.getValue() + "\""
                );
            }
            if (skipResolvedReferences) {
                return;
            }
        }
        this.factoryMethodReferences.put(methodName, new XmlMethodReference(45314913602850L, beanName, attribute, referenceDescription, null));
    }

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        Map map3 = (Map) object;
        Map map2 = (Map) object1;
        TwoKeyMap twoKeyMap = (TwoKeyMap) object2;
        String tagNameValue = xmlElementNode.getTagName();
        String openElementName = this.getOpenElementName(1);
        if (tagNameValue.equalsIgnoreCase("bean")) {
            String string2 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag";
            String dottedClassName = null;
            ProgramClass programClass1 = null;
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("class");
            if (observableHolder != null) {
                String valueValue = (String) observableHolder.getValue();
                dottedClassName = this.addClassReference(valueValue, map1, string2 + " '" + "class" + "' attribute");
                if (dottedClassName != null) {
                    programClass1 = ClassHierarchyNode.findProgramClass(ZkmUtils.dotsToSlashes(dottedClassName));
                }
            }

            ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("id");
            if (observableHolder1 != null) {
                String valueValue2 = (String) observableHolder1.getValue();
                if (programClass1 != null) {
                    this.beanClasses.put(valueValue2, programClass1);
                }
            }

            String valueValue3 = null;
            ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue("factory-ref");
            if (observableHolder2 != null) {
                valueValue3 = (String) observableHolder2.getValue();
                if (programClass1 == null) {
                    programClass1 = (ProgramClass) this.beanClasses.get(valueValue3);
                    if (programClass1 != null) {
                        dottedClassName = programClass1.getDottedClassName();
                    }
                }
            }

            Enumeration enumeration = xmlElementNode.getAttributeNames();

            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                String valueValue4 = (String) xmlElementNode.getAttributeValue(key).getValue();
                String valueValue14 = string2 + " '" + key + "' attribute";
                if (!key.equalsIgnoreCase("class") && !key.equalsIgnoreCase("id") && !key.equalsIgnoreCase("factory-ref")) {
                    if (key.equalsIgnoreCase("init-method")) {
                        MethodSignature methodSignature1 = new MethodSignature(valueValue4, "()V");
                        this.addMethodReference(dottedClassName, methodSignature1, map2, valueValue14);
                    } else if (key.equalsIgnoreCase("destroy-method")) {
                        MethodSignature methodSignature2 = new MethodSignature(valueValue4, "()V");
                        this.addMethodReference(dottedClassName, methodSignature2, map2, valueValue14);
                    } else if (key.equalsIgnoreCase("factory-method")) {
                        if (programClass1 != null) {
                            this.addHierarchyMethodReferences(
                                    programClass1, valueValue4, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map2, twoKeyMap, string2
                            );
                        } else if (valueValue3 != null) {
                            this.factoryMethodReferences.put(valueValue4, new XmlMethodReference(valueValue3, string2));
                        }
                    }
                }
            }
        } else if (tagNameValue.equalsIgnoreCase("service")) {
            ObservableHolder observableHolder3 = xmlElementNode.getAttributeValue("interface");
            if (observableHolder3 != null) {
                String valueValue5 = (String) observableHolder3.getValue();
                String string16 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag";
                this.addClassReference(valueValue5, map1, string16 + " '" + "class" + "' attribute");
            }
        } else if (tagNameValue.equalsIgnoreCase("registration-listener")) {
            ObservableHolder observableHolder4 = xmlElementNode.getAttributeValue("ref");
            if (observableHolder4 != null) {
                String valueValue6 = (String) observableHolder4.getValue();
                ProgramClass programClass2 = (ProgramClass) this.beanClasses.get(valueValue6);
                String string21 = "reference in '" + super.resourceName + "' by '" + "registration-listener" + "' tag";
                ObservableHolder observableHolder8 = xmlElementNode.getAttributeValue("registration-method");
                XmlElementNode xmlElementNode2;
                String string30;
                if (observableHolder8 != null) {
                    String valueValue7 = (String) observableHolder8.getValue();
                    if (programClass2 != null) {
                        this.addHierarchyMethodReferences(
                                programClass2, valueValue7, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map2, twoKeyMap, string21
                        );
                        xmlElementNode2 = xmlElementNode;
                        long bf = 24856730383790L;
                        string30 = "unregistration-method";
                    } else {
                        this.factoryMethodReferences.put(valueValue7, new XmlMethodReference(valueValue6, string21));
                        xmlElementNode2 = xmlElementNode;
                        long ba = 24856730383790L;
                        string30 = "unregistration-method";
                    }
                } else {
                    xmlElementNode2 = xmlElementNode;
                    long bb = 24856730383790L;
                    string30 = "unregistration-method";
                }

                String keyValue = string30;
                ObservableHolder observableHolder10 = xmlElementNode2.getAttributeValue(keyValue);
                if (observableHolder10 != null) {
                    String valueValue8 = (String) observableHolder10.getValue();
                    if (programClass2 != null) {
                        this.addHierarchyMethodReferences(
                                programClass2, valueValue8, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map2, twoKeyMap, string21
                        );
                    } else {
                        this.factoryMethodReferences.put(valueValue8, new XmlMethodReference(valueValue6, string21));
                    }
                }
            }
        } else if (tagNameValue.equalsIgnoreCase("reference-listener")) {
            ObservableHolder observableHolder5 = xmlElementNode.getAttributeValue("ref");
            if (observableHolder5 != null) {
                String valueValue9 = (String) observableHolder5.getValue();
                ProgramClass programClass3 = (ProgramClass) this.beanClasses.get(valueValue9);
                String string22 = "reference in '" + super.resourceName + "' by '" + "reference-listener" + "' tag";
                ObservableHolder observableHolder9 = xmlElementNode.getAttributeValue("bind-method");
                XmlElementNode xmlElementNode1;
                String string29;
                if (observableHolder9 != null) {
                    String valueValue10 = (String) observableHolder9.getValue();
                    if (programClass3 != null) {
                        this.addHierarchyMethodReferences(
                                programClass3, valueValue10, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map2, twoKeyMap, string22
                        );
                        xmlElementNode1 = xmlElementNode;
                        long bc = 24856730383790L;
                        string29 = "unbind-method";
                    } else {
                        this.factoryMethodReferences.put(valueValue10, new XmlMethodReference(valueValue9, string22));
                        xmlElementNode1 = xmlElementNode;
                        long bd = 24856730383790L;
                        string29 = "unbind-method";
                    }
                } else {
                    xmlElementNode1 = xmlElementNode;
                    long be = 24856730383790L;
                    string29 = "unbind-method";
                }

                String keyValue2 = string29;
                ObservableHolder observableHolder11 = xmlElementNode1.getAttributeValue(keyValue2);
                if (observableHolder11 != null) {
                    String valueValue11 = (String) observableHolder11.getValue();
                    if (programClass3 != null) {
                        this.addHierarchyMethodReferences(
                                programClass3, valueValue11, (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map2, twoKeyMap, string22
                        );
                    } else {
                        this.factoryMethodReferences.put(valueValue11, new XmlMethodReference(valueValue9, string22));
                    }
                }
            }
        } else if (tagNameValue.equalsIgnoreCase("reference")) {
            ObservableHolder observableHolder6 = xmlElementNode.getAttributeValue("interface");
            if (observableHolder6 != null) {
                String valueValue12 = (String) observableHolder6.getValue();
                String string19 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag";
                this.addClassReference(valueValue12, map1, string19 + " '" + "class" + "' attribute");
            }
        } else if (tagNameValue.equalsIgnoreCase("property") && openElementName != null && openElementName.equalsIgnoreCase("bean")) {
            ObservableHolder observableHolder7 = xmlElementNode.getAttributeValue("name");
            if (observableHolder7 != null) {
                String valueValue13 = (String) observableHolder7.getValue();
                String valueValue16 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "name" + "' attribute";
                String string23 = this.findEnclosingBeanClassName();
                if (string23 != null) {
                    ProgramClass programClass5 = this.findProgramClass(string23);
                    if (programClass5 != null) {
                        this.addPropertyAccessorReferences(programClass5, valueValue13, map2, twoKeyMap, valueValue16);
                    }
                }
            }
        } else if (tagNameValue.equalsIgnoreCase("argument")) {
            String valueValue15 = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag '" + "argument" + "' attribute";
            String string14 = this.findEnclosingBeanClassName();
            if (string14 != null) {
                ProgramClass programClass4 = this.findProgramClass(string14);
                if (programClass4 != null) {
                    this.addHierarchyMethodReferences(
                            programClass4, "<init>", (String) null, null, BeanPropertyXmlHandler.ANY_METHOD_DESCRIPTOR, 0, map2, twoKeyMap, valueValue15
                    );
                }
            }
        } else {
            super.analyzeElement(xmlElementNode, map1, map3, map2, twoKeyMap);
        }
    }

    @Override
    public boolean shouldFlushOutput() {
        return false;
    }

    public String findEnclosingBeanClassName() {
        int ba = 0;
        int bb = 0;

        for (ObjectStack objectStack = super.elementStack; bb < objectStack.size(); objectStack = super.elementStack) {
            XmlElementNode xmlElementNode = (XmlElementNode) super.elementStack.peekAt(ba);
            ObservableHolder observableHolder;
            if (xmlElementNode.getTagName().equals("bean") && (observableHolder = xmlElementNode.getAttributeValue("class")) != null) {
                return (String) observableHolder.getValue();
            }

            bb = ++ba;
        }

        return null;
    }
}
