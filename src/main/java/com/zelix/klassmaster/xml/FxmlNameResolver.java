package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.archive.ZkmFileUtils;
import com.zelix.klassmaster.classfile.ClassFileBase;
import com.zelix.klassmaster.classfile.ProgramClass;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;

public class FxmlNameResolver implements XmlClassReferenceHandler {
    public final FxmlFileUpdater fxmlUpdater;

    @Override
    public void handleFactoryAttribute(Object object, ProgramClass programClass1, Object object1, ObservableHolder observableHolder) throws ZkmException, IOException {
        String value = (String) observableHolder.getValue();
        String valueValue = this.fxmlUpdater.lookupRenamedMethodName(programClass1.getDottedClassName(), value, BeanPropertyXmlHandler.GETTER_DESCRIPTOR, 8);
        if (valueValue != null && !valueValue.equals(value)) {
            observableHolder.setValue(valueValue);
        }
    }

    @Override
    public void handleQualifiedClassTag(Object object, ProgramClass programClass1) throws ZkmException, IOException {
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        xmlElementNode.getTagName();
        xmlElementNode.setTagName(programClass1.getDottedClassName());
    }

    @Override
    public boolean handleControllerFieldId(
            XmlElementNode xmlElementNode, ProgramClass programClass1, ClassFileBase classFileBase, Object object, String name, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        xmlElementNode.getTagName();
        String renamedTypedFieldName = this.fxmlUpdater.getRenamedTypedFieldName(programClass1, classFileBase, name);
        if (renamedTypedFieldName != null && !renamedTypedFieldName.equals(name)) {
            observableHolder.setValue(renamedTypedFieldName);
        }

        return renamedTypedFieldName != null;
    }

    @Override
    public void handleResolvedClassElement(Object object, Object object1) {
    }

    @Override
    public void handleStaticPropertyAttribute(XmlElementNode xmlElementNode, ProgramClass programClass1, String key, Object object, String value) throws ZkmException, IOException {
        String string2 = (String) object;
        xmlElementNode.getTagName();
        ObservableHolder observableHolder = new ObservableHolder(value);
        String string3 = (String) ZkmUtils.mapOrSelf(programClass1.getClassName(), FxmlFileUpdater.getOriginalClassNames(this.fxmlUpdater));
        this.fxmlUpdater.resolveRenamedPropertyName(programClass1.getDottedClassName(), string3, value, observableHolder);
        if (!((String) observableHolder.getValue()).equals(value) || !programClass1.getClassName().equals(string3)) {
            String valueValue = ZkmUtils.concatStrings(
                    new String[]{
                            string2.indexOf(46) == -1 ? programClass1.getSimpleName() : programClass1.getDottedClassName(), ".", (String) observableHolder.getValue()
                    }
            );
            xmlElementNode.renameAttribute(key, valueValue);
        }
    }

    @Override
    public void handleSimpleClassTag(Object object, ProgramClass programClass1) throws ZkmException, IOException {
        XmlElementNode xmlElementNode = (XmlElementNode) object;
        xmlElementNode.getTagName();
        xmlElementNode.setTagName(programClass1.getSimpleName());
    }

    @Override
    public boolean handleControllerEventHandler(
            XmlElementNode xmlElementNode, ProgramClass programClass1, Object object, String string, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        xmlElementNode.getTagName();
        String string1 = this.fxmlUpdater
                .lookupRenamedMethodName(programClass1.getDottedClassName(), string, FxmlFileUpdater.getHandlerSignatureFilter(this.fxmlUpdater), 0);
        if (string1 != null && !string1.equals(string)) {
            String value = ZkmUtils.concatStrings(new String[]{String.valueOf('#'), string1});
            observableHolder.setValue(value);
        }

        return string1 != null;
    }

    @Override
    public void handleResourceLocation(Object object, Object object1) throws ZkmException, IOException {
        ObservableHolder observableHolder = (ObservableHolder) object1;
        FxmlFileUpdater.getEntryName(this.fxmlUpdater);
        String parentPath = ZkmFileUtils.getParentPath(
                this.fxmlUpdater.resourcePathTranslator.translateResourcePath(FxmlFileUpdater.getEntryPathName(this.fxmlUpdater))
        );
        FxmlFileUpdater fxmlFileUpdater;
        if (parentPath == null) {
            parentPath = "";
            fxmlFileUpdater = this.fxmlUpdater;
        } else {
            fxmlFileUpdater = this.fxmlUpdater;
        }

        String parentPathValue = ZkmFileUtils.getParentPath(FxmlFileUpdater.getOriginalEntryName(fxmlFileUpdater));
        if (parentPathValue == null) {
            parentPathValue = "";
        }

        String string2 = ((String) object).substring(1);
        String string3 = ZkmFileUtils.resolveAgainstBase(string2, parentPathValue);
        int ba = string3.indexOf(58);
        if (ba > -1) {
            if (ba < string3.length() - 1) {
                string3 = string3.substring(ba + 1);
                fxmlFileUpdater = this.fxmlUpdater;
            } else {
                fxmlFileUpdater = this.fxmlUpdater;
            }
        } else {
            fxmlFileUpdater = this.fxmlUpdater;
        }

        String string4 = fxmlFileUpdater.resourcePathTranslator.translateResourcePath(string3);
        String string5 = string4;
        if (string2.charAt(0) != '/') {
            string5 = ZkmFileUtils.computeRelativePath(parentPath, string4);
        }

        if (!string5.equals(string2)) {
            observableHolder.setValue('@' + string5);
        }
    }

    @Override
    public void handlePropertyTag(XmlElementNode xmlElementNode, ProgramClass programClass1) throws ZkmException, IOException {
        String tagName = xmlElementNode.getTagName();
        ObservableHolder observableHolder = new ObservableHolder(tagName);
        this.fxmlUpdater
                .resolveRenamedPropertyName(
                        programClass1.getDottedClassName(),
                        (String) ZkmUtils.mapOrSelf(programClass1.getClassName(), FxmlFileUpdater.getOriginalClassNames(this.fxmlUpdater)),
                        tagName,
                        observableHolder
                );
        if (!((String) observableHolder.getValue()).equals(tagName)) {
            xmlElementNode.setTagName((String) observableHolder.getValue());
        }
    }

    @Override
    public void handleControllerAttribute(Object object2, Object object, Object object3, Object object1) throws ZkmException, IOException {
        ProgramClass programClass1 = (ProgramClass) object;
        ((ObservableHolder) object1).setValue(programClass1.getDottedClassName());
    }

    @Override
    public void reportWarning(Object object, Object object1) {
        String string = (String) object1;
        String string1 = (String) object;
        this.fxmlUpdater.messageReporter.reportWarning(string1, string);
    }

    @Override
    public void handleStaticPropertyTag(XmlElementNode xmlElementNode, ProgramClass programClass1, Object object, String string) throws ZkmException, IOException {
        String renamedFieldName = this.fxmlUpdater.getRenamedFieldName(programClass1.getClassName(), string);
        if (!programClass1.getSimpleName().equals(object) || !string.equals(renamedFieldName)) {
            String tagName = ZkmUtils.concatStrings(new String[]{programClass1.getSimpleName(), ".", renamedFieldName});
            xmlElementNode.setTagName(tagName);
        }
    }

    @Override
    public boolean handleAnnotatedEventHandler(XmlElementNode xmlElementNode, Object object, String key, ObservableHolder observableHolder) throws ZkmException, IOException {
        xmlElementNode.getTagName();
        ObservableHolder observableHolder1 = new ObservableHolder();
        String string1 = this.fxmlUpdater.resolveRenamedMethodByName(key, FxmlFileUpdater.getHandlerSignatureFilter(this.fxmlUpdater), observableHolder1);
        if (string1 != null && !string1.equals(key)) {
            String value = ZkmUtils.concatStrings(new String[]{String.valueOf('#'), string1});
            observableHolder.setValue(value);
        }

        return string1 != null;
    }

    public FxmlNameResolver(FxmlFileUpdater fxmlFileUpdater) {
        this.fxmlUpdater = fxmlFileUpdater;
    }

    @Override
    public boolean handleAnnotatedFieldId(
            XmlElementNode xmlElementNode, ClassFileBase classFileBase, Object object, String name, ObservableHolder observableHolder
    ) throws ZkmException, IOException {
        xmlElementNode.getTagName();
        ObservableHolder observableHolder1 = new ObservableHolder();
        String value = this.fxmlUpdater.resolveRenamedFieldName(classFileBase, name, observableHolder1);
        if (value != null && !value.equals(name)) {
            observableHolder.setValue(value);
        }

        return value != null;
    }

    @Override
    public void handlePropertyAttribute(XmlElementNode xmlElementNode, ProgramClass programClass1, String string) throws ZkmException, IOException {
        xmlElementNode.getTagName();
        ObservableHolder observableHolder = new ObservableHolder(string);
        this.fxmlUpdater
                .resolveRenamedPropertyName(
                        programClass1.getDottedClassName(),
                        (String) ZkmUtils.mapOrSelf(programClass1.getClassName(), FxmlFileUpdater.getOriginalClassNames(this.fxmlUpdater)),
                        string,
                        observableHolder
                );
        if (!((String) observableHolder.getValue()).equals(string)) {
            xmlElementNode.renameAttribute(string, (String) observableHolder.getValue());
        }
    }

    @Override
    public void handleConstantAttribute(Object object, ProgramClass programClass1, Object object1, ObservableHolder observableHolder) throws ZkmException, IOException {
        String value = (String) observableHolder.getValue();
        String renamedFieldName = this.fxmlUpdater.getRenamedFieldName(programClass1.getDottedClassName(), value);
        if (!renamedFieldName.equals(value)) {
            observableHolder.setValue(renamedFieldName);
        }
    }
}
