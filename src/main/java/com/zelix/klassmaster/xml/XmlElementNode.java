package com.zelix.klassmaster.xml;

import com.zelix.klassmaster.config.HiddenOptionFlags;
import com.zelix.klassmaster.exceptions.ZkmException;
import com.zelix.klassmaster.util.EmptyEnumeration;
import com.zelix.klassmaster.util.ObservableHolder;
import com.zelix.klassmaster.util.ZkmUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class XmlElementNode {
    private static final String NO_NAME_MESSAGE = "Element or attribute has no name : '";
    public ObservableHolder tagNameHolder;
    public ObservableHolder currentAttributeNameHolder;
    public Map attributeNameHolders;
    public String originalTagName;
    public Map attributeValues;
    public List tokens = new ArrayList(10);

    public void addAttributeValue(String string) throws ZkmException, IOException {
        String valueValue = string;
        if (this.currentAttributeNameHolder == null) {
            throw new XmlContentException(NO_NAME_MESSAGE + valueValue + "'");
        }

        if (HiddenOptionFlags.TRIM_XML_TEXT) {
            valueValue = valueValue.trim();
        }

        ObservableHolder observableHolder = new ObservableHolder(valueValue);
        String value = (String) this.currentAttributeNameHolder.getValue();
        Map map1;
        if (this.attributeNameHolders == null) {
            this.attributeNameHolders = ZkmUtils.createHashMap(13);
            this.attributeValues = ZkmUtils.createHashMap(13);
            map1 = this.attributeNameHolders;
        } else {
            map1 = this.attributeNameHolders;
        }

        map1.put(value, this.currentAttributeNameHolder);
        this.attributeValues.put(value, observableHolder);
        this.tokens.add(observableHolder);
    }

    public void addRawText(String string) {
        if (string.length() > 0) {
            this.tokens.add(string);
        }
    }

    public boolean renameAttribute(String key, String value) throws ZkmException, IOException {
        if (this.attributeValues != null) {
            ObservableHolder observableHolder = (ObservableHolder) this.attributeValues.remove(key);
            if (observableHolder != null) {
                this.attributeValues.put(value, observableHolder);
                ((ObservableHolder) this.attributeNameHolders.get(key)).setValue(value);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public ObservableHolder getAttributeValue(Object key) {
        return this.attributeValues != null ? (ObservableHolder) this.attributeValues.get(key) : null;
    }

    public Enumeration getAttributeNames() {
        return this.attributeValues != null ? Collections.enumeration(new ArrayList(this.attributeValues.keySet())) : new EmptyEnumeration();
    }

    public void addName(String originalTagNameArgument) throws ZkmException, IOException {
        ObservableHolder observableHolder = new ObservableHolder(originalTagNameArgument);
        List list1;
        if (this.tagNameHolder == null) {
            this.originalTagName = originalTagNameArgument;
            this.tagNameHolder = observableHolder;
            list1 = this.tokens;
        } else {
            this.currentAttributeNameHolder = observableHolder;
            list1 = this.tokens;
        }

        list1.add(observableHolder);
    }

    public String getOriginalTagName() {
        return this.originalTagName;
    }

    public void setTagName(Object tagName) throws ZkmException, IOException {
        this.tagNameHolder.setValue(tagName);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean expandHolders = XmlResourceParserBase.getPredicateFlag_boolean();
        int tokenCount = this.tokens.size();
        for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
            Object token = this.tokens.get(tokenIndex);
            if (expandHolders && token instanceof ObservableHolder) {
                result.append((String) ((ObservableHolder) token).getValue());
            } else {
                result.append(token);
            }
            if (!expandHolders) {
                break;
            }
        }
        return result.toString();
    }

    public String getTagName() {
        boolean predicateFlagClear = XmlResourceParserBase.isPredicateFlagClear();
        Object value = this;
        if (!predicateFlagClear) {
            if (this.tagNameHolder == null) {
                return null;
            }

            value = this.tagNameHolder.getValue();
        }

        return (String) value;
    }
}
