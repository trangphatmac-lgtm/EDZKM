package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmAssert;

public class XmlMethodReference {
    public ObservableHolder methodNameHolder;
    public String beanName;
    public String referenceDescription;

    public static ObservableHolder getMethodNameHolderOf(XmlMethodReference xmlMethodReference) {
        return xmlMethodReference.getMethodNameHolder();
    }

    public static String getReferenceDescriptionOf(XmlMethodReference xmlMethodReference) {
        return xmlMethodReference.getReferenceDescription();
    }

    @Override
    public String toString() {
        return "<" + ZkmAssert.getSimpleClassName(this) + " " + this.beanName + ", " + this.methodNameHolder + ", " + this.referenceDescription + ", >";
    }

    public String getReferenceDescription() {
        return this.referenceDescription;
    }

    public static String getBeanNameOf(XmlMethodReference xmlMethodReference) {
        return xmlMethodReference.getBeanName();
    }

    public ObservableHolder getMethodNameHolder() {
        return this.methodNameHolder;
    }

    public XmlMethodReference(long ba, String beanNameArgument, ObservableHolder observableHolder, String referenceDescriptionArgument, SyntheticAccessMarker syntheticAccessMarker) {
        this(beanNameArgument, observableHolder, referenceDescriptionArgument);
    }

    public XmlMethodReference(String beanNameArgument, String referenceDescriptionArgument) {
        this.beanName = beanNameArgument;
        this.referenceDescription = referenceDescriptionArgument;
    }

    public String getBeanName() {
        return this.beanName;
    }

    private XmlMethodReference(String beanNameArgument, ObservableHolder observableHolder, String referenceDescriptionArgument) {
        this.beanName = beanNameArgument;
        this.referenceDescription = referenceDescriptionArgument;
        this.methodNameHolder = observableHolder;
    }
}
