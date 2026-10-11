package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.classfile.ClassFileBase;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FxmlFileUpdater extends BeanPropertyXmlHandler {
    public XmlClassReferenceHandler referenceHandler;
    public ProgramClass controllerClass;
    public Set wildcardImportPackages = ZkmUtils.createHashSet();
    public Set importedClassNames = ZkmUtils.createHashSet();
    public Map elementClasses = ZkmUtils.createHashMap();
    public Map resolvedClassCache = ZkmUtils.createHashMap();
    public Map elementsById = ZkmUtils.createHashMap();
    public String fxNamespacePrefix = "fx:";
    public EnumerableMap originalClassNames;
    public final JavaFxHandlerSignatureFilter handlerSignatureFilter;

    public FxmlFileUpdater(
            String string,
            EnumerableMap enumerableMap,
            EnumerableMap enumerableMap1,
            ReadOnlyMultiMap readOnlyMultiMap,
            ReadOnlyMultiMap readOnlyMultiMap1,
            ResourcePathTranslator resourcePathTranslator1,
            ClassMemberLookup classMemberLookup1,
            ClassResolver classResolver1,
            MessageReporter messageReporter1
    ) {
        super(string, enumerableMap, readOnlyMultiMap, readOnlyMultiMap1, resourcePathTranslator1, classMemberLookup1, classResolver1, messageReporter1);
        this.originalClassNames = enumerableMap1;
        this.handlerSignatureFilter = new JavaFxHandlerSignatureFilter(classMemberLookup1, messageReporter1);
    }

    public ClassFileBase resolveImportedClass(String string) throws ZkmException, IOException {
        ClassFileBase classFileBase = (ClassFileBase) this.resolvedClassCache.get(string);
        if (classFileBase != null) {
            return classFileBase;
        }

        classFileBase = this.resolveClass(string);
        if (classFileBase != null) {
            this.resolvedClassCache.put(string, classFileBase);
            return classFileBase;
        }

        Iterator iterator = this.importedClassNames.iterator();

        while (iterator.hasNext()) {
            String string1 = (String) iterator.next();
            if (string1.endsWith(string) && string1.charAt(string1.length() - string.length() - 1) == '/') {
                classFileBase = this.resolveClass(string1);
                if (classFileBase != null) {
                    this.resolvedClassCache.put(string, classFileBase);
                    return classFileBase;
                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        Iterator iterator1 = this.wildcardImportPackages.iterator();

        while (iterator1.hasNext()) {
            String string2 = (String) iterator1.next();
            stringBuilder.setLength(0);
            stringBuilder.append(string2);
            stringBuilder.append(string);
            classFileBase = this.resolveClass(stringBuilder.toString());
            if (classFileBase != null) {
                this.resolvedClassCache.put(string, classFileBase);
                return classFileBase;
            }
        }

        return null;
    }

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        if (this.referenceHandler == null) {
            this.referenceHandler = new FxmlReferenceHandler(this, map1, (Map) object, (Map) object1, (TwoKeyMap) object2);
        }

        this.processElement(xmlElementNode);
    }

    public void processElement(XmlElementNode xmlElementNode) throws ZkmException, IOException {
        String tagName = xmlElementNode.getTagName();
        String openElementName = this.getOpenElementName(1);
        if (openElementName == null) {
            this.detectFxNamespacePrefix(xmlElementNode);
            ObservableHolder observableHolder = new ObservableHolder();
            String string2 = this.findControllerClassName(xmlElementNode, observableHolder);
            if (string2 != null) {
                this.controllerClass = this.findProgramClass(string2);
                if (this.controllerClass != null) {
                    ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue((String) observableHolder.getValue());
                    this.referenceHandler.handleControllerAttribute(xmlElementNode, this.controllerClass, (String) observableHolder.getValue(), observableHolder1);
                }
            }
        }

        ClassFileBase classFileBase = null;
        int endIndex = tagName.indexOf(46);
        int bb = tagName.lastIndexOf(46);
        if (tagName.startsWith(this.fxNamespacePrefix)) {
            String string3 = tagName.substring(tagName.indexOf(58) + 1);
            if (string3.equals("root")) {
                ObservableHolder observableHolder2 = xmlElementNode.getAttributeValue("type");
                if (observableHolder2 != null) {
                    String value = (String) observableHolder2.getValue();
                    if (value != null && value.length() > 0) {
                        if (value.indexOf(46) > 0) {
                            classFileBase = this.resolveClass(value);
                        } else {
                            classFileBase = this.resolveImportedClass(value);
                        }
                    }
                }
            } else if (string3.equals("reference") || string3.equals("copy")) {
                ObservableHolder sourceAttribute = xmlElementNode.getAttributeValue("source");
                String sourceId = (String) sourceAttribute.getValue();
                if (sourceId != null && sourceId.length() > 0) {
                    XmlElementNode sourceElement = (XmlElementNode) this.elementsById.get(sourceId);
                    if (sourceElement != null) {
                        classFileBase = (ClassFileBase) this.elementClasses.get(sourceElement);
                    }
                }
            } else if (string3.equals("script")) {
                this.referenceHandler.reportWarning(
                        "WARNING:", "FXML file '" + super.resourceName + "' contains the '" + tagName + "' tag. Its contents will not be changed."
                );
            }
        } else if (endIndex > 0 && bb < tagName.length() - 1) {
            if (Character.isUpperCase(tagName.charAt(0)) && endIndex == bb && Character.isLowerCase(tagName.charAt(endIndex + 1))) {
                String string7 = tagName.substring(0, endIndex);
                classFileBase = this.resolveImportedClass(string7);
                if (classFileBase != null && classFileBase.isProgramClass()) {
                    String string8 = tagName.substring(endIndex + 1);
                    this.referenceHandler.handleStaticPropertyTag(xmlElementNode, (ProgramClass) classFileBase, string7, string8);
                }
            } else {
                classFileBase = this.resolveClass(tagName);
                if (classFileBase != null && classFileBase.isProgramClass()) {
                    this.referenceHandler.handleQualifiedClassTag(xmlElementNode, (ProgramClass) classFileBase);
                }
            }
        } else if (Character.isUpperCase(tagName.charAt(0))) {
            classFileBase = this.resolveImportedClass(tagName);
            if (classFileBase != null && classFileBase.isProgramClass()) {
                this.referenceHandler.handleSimpleClassTag(xmlElementNode, (ProgramClass) classFileBase);
            }
        } else if (openElementName != null) {
            ClassFileBase classFileBase1 = this.findEnclosingElementClass();
            if (classFileBase1 != null && classFileBase1.isProgramClass()) {
                this.referenceHandler.handlePropertyTag(xmlElementNode, (ProgramClass) classFileBase1);
            }
        }

        if (classFileBase != null) {
            this.elementClasses.put(xmlElementNode, classFileBase);
            if (classFileBase.isProgramClass()) {
                this.referenceHandler.handleResolvedClassElement(xmlElementNode, (ProgramClass) classFileBase);
            }

            Enumeration enumeration = xmlElementNode.getAttributeNames();

            while (enumeration.hasMoreElements()) {
                String keyValue = (String) enumeration.nextElement();
                ObservableHolder observableHolder4 = xmlElementNode.getAttributeValue(keyValue);
                String valueValue = (String) observableHolder4.getValue();
                if (keyValue.startsWith(this.fxNamespacePrefix)) {
                    String key = keyValue.substring(keyValue.indexOf(58) + 1);
                    if (key.equals("id")) {
                        this.elementsById.put(key, xmlElementNode);
                        if (classFileBase != null) {
                            this.processIdAttribute(xmlElementNode, classFileBase, keyValue, valueValue, observableHolder4);
                        }
                    } else if (key.equals("constant")) {
                        if (classFileBase.isProgramClass()) {
                            this.referenceHandler.handleConstantAttribute(xmlElementNode, (ProgramClass) classFileBase, keyValue, observableHolder4);
                        }
                    } else if (key.equals("factory") && classFileBase.isProgramClass()) {
                        this.referenceHandler.handleFactoryAttribute(xmlElementNode, (ProgramClass) classFileBase, keyValue, observableHolder4);
                    }
                } else if (keyValue.indexOf(46) > -1) {
                    this.processStaticPropertyAttribute(xmlElementNode, keyValue);
                } else {
                    if (classFileBase.isProgramClass()) {
                        this.referenceHandler.handlePropertyAttribute(xmlElementNode, (ProgramClass) classFileBase, keyValue);
                    }

                    if (valueValue.length() > 1) {
                        if (valueValue.charAt(0) == '#') {
                            this.processEventHandlerAttribute(xmlElementNode, keyValue, valueValue, observableHolder4);
                        } else if (valueValue.charAt(0) == '@') {
                            this.referenceHandler.handleResourceLocation(valueValue, observableHolder4);
                        } else if (valueValue.charAt(0) == '$') {
                        }
                    }
                }
            }
        }
    }

    public void processIdAttribute(XmlElementNode xmlElementNode, ClassFileBase classFileBase, String string, String string1, ObservableHolder observableHolder) throws ZkmException, IOException {
        boolean bl = false;
        if (this.controllerClass != null) {
            bl = this.referenceHandler.handleControllerFieldId(xmlElementNode, this.controllerClass, classFileBase, string, string1, observableHolder);
        }

        if (!bl) {
            this.referenceHandler.handleAnnotatedFieldId(xmlElementNode, classFileBase, string, string1, observableHolder);
        }
    }

    @Override
    public final void analyzeProcessingInstruction(Object object) {
        String string = (String) object;
        if (string.startsWith("import")) {
            this.processImport(string, false);
        }
    }

    public void processStaticPropertyAttribute(XmlElementNode xmlElementNode, String string) throws ZkmException, IOException {
        int ba = string.indexOf(46);
        int endIndex = string.lastIndexOf(46);
        if (ba > 0 && endIndex < string.length() - 1) {
            String string1 = string.substring(0, endIndex);
            ClassFileBase classFileBase;
            if (string1.indexOf(46) == -1) {
                classFileBase = this.resolveImportedClass(string1);
            } else {
                classFileBase = this.resolveClass(string1);
            }

            if (classFileBase != null && classFileBase.isProgramClass()) {
                String string2 = string.substring(endIndex + 1);
                this.referenceHandler.handleStaticPropertyAttribute(xmlElementNode, (ProgramClass) classFileBase, string, string1, string2);
            }
        }
    }

    public String processImport(String string, boolean bl) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("import");
        stringBuilder.append(' ');
        String renamedClassName = string.substring("import".length()).trim();
        if (renamedClassName.indexOf(42) > -1) {
            int endIndex = renamedClassName.lastIndexOf(46);
            this.wildcardImportPackages.add(renamedClassName.substring(0, endIndex + 1).replace('.', '/'));
            if (bl) {
                String string2 = this.renameDirectoryPath(renamedClassName.substring(0, endIndex));
                renamedClassName = string2 + ".*";
            }
        } else {
            this.importedClassNames.add(renamedClassName.replace('.', '/'));
            if (bl) {
                renamedClassName = this.getRenamedClassName(renamedClassName);
            }
        }

        stringBuilder.append(renamedClassName);
        return stringBuilder.toString();
    }

    public static EnumerableMap getOriginalClassNames(FxmlFileUpdater fxmlFileUpdater) {
        return fxmlFileUpdater.originalClassNames;
    }

    public static JavaFxHandlerSignatureFilter getHandlerSignatureFilter(FxmlFileUpdater fxmlFileUpdater) {
        return fxmlFileUpdater.handlerSignatureFilter;
    }

    public String findControllerClassName(XmlElementNode xmlElementNode, ObservableHolder observableHolder) throws ZkmException, IOException {
        String value = this.fxNamespacePrefix + "controller";
        observableHolder.setValue(value);
        Enumeration enumeration = xmlElementNode.getAttributeNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            if (key.equals(value)) {
                return (String) xmlElementNode.getAttributeValue(key).getValue();
            }
        }

        return null;
    }

    public void processEventHandlerAttribute(XmlElementNode xmlElementNode, String string, String string1, ObservableHolder observableHolder) throws ZkmException, IOException {
        String string2 = string1.substring(1);
        boolean bl = false;
        if (this.controllerClass != null) {
            bl = this.referenceHandler.handleControllerEventHandler(xmlElementNode, this.controllerClass, string, string2, observableHolder);
        }

        if (!bl) {
            this.referenceHandler.handleAnnotatedEventHandler(xmlElementNode, string, string2, observableHolder);
        }
    }

    public FxmlFileUpdater(String string, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1) {
        super(string, classMemberLookup1, classResolver1, messageReporter1);
        this.handlerSignatureFilter = new JavaFxHandlerSignatureFilter(classMemberLookup1, messageReporter1);
    }

    @Override
    public void rewriteElement(Object object, Object object1) throws ZkmException, IOException {
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        if (this.referenceHandler == null) {
            this.referenceHandler = new FxmlNameResolver(this);
        }

        this.processElement(xmlElementNode);
        ((List) object1).add(xmlElementNode);
    }

    public static String getEntryName(FxmlFileUpdater fxmlFileUpdater) {
        return fxmlFileUpdater.entryName;
    }

    public ClassFileBase findEnclosingElementClass() {
        int ba = 1;

        XmlElementNode xmlElementNode;
        do {
            xmlElementNode = this.getOpenElement(ba);
            if (xmlElementNode != null) {
                ClassFileBase classFileBase = (ClassFileBase) this.elementClasses.get(xmlElementNode);
                if (classFileBase != null) {
                    return classFileBase;
                }
            }

            ba++;
        } while (xmlElementNode != null);

        return null;
    }

    public static String getOriginalEntryName(FxmlFileUpdater fxmlFileUpdater) {
        return fxmlFileUpdater.entryName;
    }

    public boolean detectFxNamespacePrefix(XmlElementNode xmlElementNode) {
        boolean bl = false;
        Enumeration enumeration = xmlElementNode.getAttributeNames();

        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            if (key.startsWith("xmlns")) {
                int ba = key.indexOf(58);
                if (ba > 0 && ba < key.length() - 1 && ((String) xmlElementNode.getAttributeValue(key).getValue()).equalsIgnoreCase("http://javafx.com/fxml")) {
                    this.fxNamespacePrefix = key.substring(ba + 1) + ':';
                    bl = true;
                }
            }
        }

        return bl;
    }

    public static String getEntryPathName(FxmlFileUpdater fxmlFileUpdater) {
        return fxmlFileUpdater.entryName;
    }

    @Override
    public void rewriteProcessingInstruction(Object object, List list1) {
        String string = (String) object;
        if (string.startsWith("import")) {
            string = this.processImport(string, true);
        }

        list1.add(string);
    }

    public static String getResourceName(FxmlFileUpdater fxmlFileUpdater) {
        return fxmlFileUpdater.resourceName;
    }
}
