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

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class AndroidManifestXmlHandler extends GenericXmlFileHandler {
    public String manifestPackage;

    @Override
    public void analyzeElement(XmlElementNode xmlElementNode, Map map1, Object object, Object object1, Object object2) throws ZkmException, IOException {
        Map map3 = (Map) object;
        TwoKeyMap twoKeyMap = (TwoKeyMap) object2;
        Map map2 = (Map) object1;
        String tagNameValue = xmlElementNode.getTagName();
        if (tagNameValue.equalsIgnoreCase("manifest")) {
            ObservableHolder observableHolder = xmlElementNode.getAttributeValue("package");
            if (observableHolder != null) {
                String manifestPackageValue = (String) observableHolder.getValue();
                if (manifestPackageValue != null && manifestPackageValue.length() > 0) {
                    this.manifestPackage = manifestPackageValue;
                }
            }
        } else if (!tagNameValue.equalsIgnoreCase("activity")
                && !tagNameValue.equalsIgnoreCase("provider")
                && !tagNameValue.equalsIgnoreCase("service")
                && !tagNameValue.equalsIgnoreCase("receiver")) {
            super.analyzeElement(xmlElementNode, map1, map3, map2, twoKeyMap);
        } else {
            String value = "reference in '" + super.resourceName + "' by '" + tagNameValue + "' tag";
            ObservableHolder observableHolder1 = xmlElementNode.getAttributeValue("android:name");
            if (observableHolder1 != null) {
                String valueValue = (String) observableHolder1.getValue();
                ProgramClass programClass1 = this.findProgramClass(valueValue);
                if (programClass1 == null && this.manifestPackage != null) {
                    programClass1 = this.findProgramClass(this.qualifyClassName(valueValue));
                }

                if (programClass1 != null) {
                    map1.put(programClass1, value);
                }
            }
        }
    }

    @Override
    public void rewriteElement(Object element, Object output) throws ZkmException, IOException {
        XmlElementNode node = (XmlElementNode) element;
        rewriteManifestReferences(node);
        ((List) output).add(node);
    }

    private void rewriteManifestReferences(XmlElementNode node) throws ZkmException, IOException {
        boolean continueAfterManifest = XmlConfigFileHandler.getPredicateFlag();
        String tagName = node.getTagName();
        if (tagName.equalsIgnoreCase("manifest")) {
            ObservableHolder packageAttribute = node.getAttributeValue("package");
            if (packageAttribute != null) {
                String packageName = (String) packageAttribute.getValue();
                if (packageName != null && packageName.length() > 0) {
                    this.manifestPackage = packageName;
                    packageAttribute.setValue(this.renameDirectoryPath(packageName));
                }
            }
            if (!continueAfterManifest) {
                return;
            }
        }

        if (!tagName.equalsIgnoreCase("activity")
                && !tagName.equalsIgnoreCase("provider")
                && !tagName.equalsIgnoreCase("service")
                && !tagName.equalsIgnoreCase("receiver")) {
            Enumeration attributeNames = node.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attributeName = (String) attributeNames.nextElement();
                ObservableHolder attribute = node.getAttributeValue(attributeName);
                attribute.setValue(this.updateReferenceValue((String) attribute.getValue(), attributeName, false));
            }
            return;
        }

        ObservableHolder classNameAttribute = node.getAttributeValue("android:name");
        if (classNameAttribute != null) {
            String className = (String) classNameAttribute.getValue();
            ProgramClass componentClass = this.findProgramClass(className);
            if (componentClass == null && this.manifestPackage != null) {
                componentClass = this.findProgramClass(this.qualifyClassName(className));
            }
            if (componentClass != null) {
                classNameAttribute.setValue(componentClass.getDottedClassName());
            }
        }
    }

    public String qualifyClassName(String string) {
        return string.startsWith(".") ? this.manifestPackage + string : this.manifestPackage + "." + string;
    }

    public AndroidManifestXmlHandler(
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

    public AndroidManifestXmlHandler(String string, ClassMemberLookup classMemberLookup1, ClassResolver classResolver1, MessageReporter messageReporter1) {
        super(string, classMemberLookup1, classResolver1, messageReporter1);
    }
}
