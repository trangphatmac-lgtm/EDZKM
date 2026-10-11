package com.zelix.klassmaster.ui.component;

import com.zelix.klassmaster.util.ZkmUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class ConstraintLayout implements LayoutManager2, LayoutConstraintMarker {
    private static int[] controlFlags;
    public static final String lineSeparator;
    public Integer defaultBottomPad;
    public Integer layoutHeight;
    public String[] constraintLines;
    public boolean layoutSizesResolved;
    public Integer minHeight;
    public Map variables;
    public boolean resolving;
    public boolean resolvePassStarted;
    public Integer layoutWidth;
    public Integer defaultRightPad;
    public Integer minWidth;
    public Map componentsByName = ZkmUtils.createHashMap();
    public Map specsByComponent = ZkmUtils.createHashMap();
    public LayoutExpression[] layoutSizeExpressions = new LayoutExpression[4];
    public Container container;

    @Override
    public void removeLayoutComponent(Component component1) {
        synchronized (component1.getTreeLock()) {
            LayoutComponentSpec layoutComponentSpec = (LayoutComponentSpec) this.specsByComponent.remove(component1);
            if (layoutComponentSpec != null) {
                this.componentsByName.remove(layoutComponentSpec.getComponentName());
            }
        }
    }

    public void setConstraints(String[] strings) {
        this.constraintLines = strings;
        this.variables = ZkmUtils.createHashMap();
        this.layoutWidth = null;
        this.layoutHeight = null;
        this.minWidth = null;
        this.minHeight = null;
        this.defaultRightPad = null;
        this.defaultBottomPad = null;
        int constraintLineIndex = 0;
        int bg = 0;

        for (String[] constraintLinesValue = this.constraintLines; bg < constraintLinesValue.length; constraintLinesValue = this.constraintLines) {
            StringTokenizer stringTokenizer = new StringTokenizer(this.constraintLines[constraintLineIndex], "=");
            int bb = stringTokenizer.countTokens();
            String[] strings1 = new String[bb];

            for (int i = 0; i < bb; i++) {
                strings1[i] = stringTokenizer.nextToken().trim();
            }

            String expressionText = strings1[bb - 1];

            for (int i = 0; i < bb - 1; i++) {
                String key = strings1[i];
                int endIndex = key.indexOf(".");
                if (endIndex == -1) {
                    this.variables.put(key, new LayoutExpression(this, expressionText));
                } else {
                    if (endIndex == key.length() - 1 || key.lastIndexOf(".") != endIndex) {
                        this.reportError("Property '" + key + "' is invalid. : '" + this.constraintLines[constraintLineIndex] + "'");
                    }

                    String keyValue = key.substring(0, endIndex).trim();
                    String string2 = key.substring(endIndex + 1).trim();
                    if (keyValue.equals("container")) {
                        this.setContainerProperty(string2, expressionText);
                    } else if (keyValue.equals("layout")) {
                        this.setLayoutProperty(string2, expressionText);
                    } else {
                        Component component1 = (Component) this.componentsByName.get(keyValue);
                        ((LayoutComponentSpec) this.specsByComponent.get(component1)).setProperty(string2, expressionText);
                    }
                }
            }

            bg = ++constraintLineIndex;
        }

        for (int layoutSizeExpressionIndex = 0; layoutSizeExpressionIndex < this.layoutSizeExpressions.length; layoutSizeExpressionIndex++) {
            if (this.layoutSizeExpressions[layoutSizeExpressionIndex] != null) {
                this.layoutSizeExpressions[layoutSizeExpressionIndex].prepare();
            }
        }

        Iterator iterator = this.specsByComponent.values().iterator();

        while (iterator.hasNext()) {
            LayoutComponentSpec layoutComponentSpec = (LayoutComponentSpec) iterator.next();
            layoutComponentSpec.validateExpressions();
        }
    }

    @Override
    public void addLayoutComponent(String key, Component component1) {
        synchronized (component1.getTreeLock()) {
            Component component2 = ((java.awt.Component) (this.componentsByName.put(key, component1)));
            Map map1;
            if (component2 != null) {
                this.reportError("Multiple components with the name '" + key + "': " + component2 + " : " + component1);
                map1 = this.specsByComponent;
            } else {
                map1 = this.specsByComponent;
            }

            map1.put(component1, new LayoutComponentSpec(key, component1, this));
        }
    }

    @Override
    public void addLayoutComponent(Component component1, Object object) {
        synchronized (component1.getTreeLock()) {
            if (!(object instanceof String)) {
                throw new IllegalArgumentException("'addLayoutComponent' constraint must be a name String");
            }

            String key = (String) object;
            Component component2 = ((java.awt.Component) (this.componentsByName.put(key, component1)));
            Map map1;
            if (component2 != null) {
                this.reportError("Multiple components with the name '" + key + "': " + component2 + " : " + component1);
                map1 = this.specsByComponent;
            } else {
                map1 = this.specsByComponent;
            }

            map1.put(component1, new LayoutComponentSpec(key, component1, this));
        }
    }


    static {
        setControlFlags();
        lineSeparator = ZkmUtils.LINE_SEPARATOR;
    }


    public String setContainerProperty(String string, String string1) {
        int ba;
        try {
            ba = Integer.parseInt(string1);
        } catch (NumberFormatException numberFormatException) {
            return "Only an integer can be assigned to 'container." + string + "'";
        }

        if (string.equals("defaultRightPad")) {
            this.defaultRightPad = ba;
        } else {
            if (!string.equals("defaultBottomPad")) {
                return "Property '" + string + "' is not valid for the '" + "container" + "' object.";
            }

            this.defaultBottomPad = ba;
        }

        return null;
    }

    @Override
    public float getLayoutAlignmentY(Container container1) {
        return 0.5F;
    }

    public LayoutExpression getVariable(Object key) {
        return (LayoutExpression) this.variables.get(key);
    }

    public void resolveComponents(Container container1, ArrayList arrayList, boolean bl) {
        this.resolvePassStarted = true;
        this.resolving = true;
        Insets insets = container1.getInsets();
        int componentCount = container1.getComponentCount();

        for (int i = 0; i < componentCount; i++) {
            arrayList.add(container1.getComponent(i));
        }

        boolean bl1 = true;

        while (bl1) {
            bl1 = false;
            ArrayList arrayList1 = ZkmUtils.copyArrayList(arrayList);

            for (int elementIndex = 0; elementIndex < arrayList1.size(); elementIndex++) {
                Component component1 = (Component) arrayList1.get(elementIndex);
                LayoutComponentSpec layoutComponentSpec = (LayoutComponentSpec) this.specsByComponent.get(component1);
                if (layoutComponentSpec != null) {
                    if (layoutComponentSpec.resolve(bl)) {
                        bl1 = true;
                    }

                    if (layoutComponentSpec.areExpressionsResolved(bl) && layoutComponentSpec.isBoundsResolved()) {
                        arrayList.remove(component1);
                        if (bl) {
                            Dimension dimension = layoutComponentSpec.getSize();
                            Point point = layoutComponentSpec.getLocation();
                            component1.setBounds(point.x + insets.left, point.y + insets.top, dimension.width, dimension.height);
                        }
                    }
                } else {
                    arrayList.remove(component1);
                }
            }
        }

        this.resolving = false;
    }

    public void parseConstraints(String string) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, ";");
        String[] constraints = new String[stringTokenizer.countTokens()];

        for (int constraintIndex = 0; constraintIndex < constraints.length; constraintIndex++) {
            constraints[constraintIndex] = stringTokenizer.nextToken().trim();
        }

        this.setConstraints(constraints);
    }

    public static String getPropertyName(int ba) {
        switch (ba) {
            case 0:
                return "width";
            case 1:
                return "height";
            case 2:
                return "top";
            case 3:
                return "left";
            case 4:
                return "bottom";
            case 5:
                return "right";
            case 6:
                return "centerX";
            case 7:
                return "centerY";
            case 8:
                return "minWidth";
            case 9:
                return "minHeight";
            case 10:
                return "defaultWidth";
            case 11:
                return "defaultHeight";
            default:
                return null;
        }
    }

    public static void setControlFlags() {
        controlFlags = null;
    }

    @Override
    public Dimension maximumLayoutSize(Container container1) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public void layoutContainer(Container container1) {
        synchronized (container1.getTreeLock()) {
            this.resolveLayoutSizes();
            if (this.constraintLines == null) {
            }

            this.resetComponentSpecs();
            ArrayList arrayList = new ArrayList(container1.getComponentCount());
            this.resolveComponents(container1, arrayList, true);
            if (arrayList.size() > 0) {
                StringBuffer stringBuffer = new StringBuffer();

                for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                    LayoutComponentSpec layoutComponentSpec = (LayoutComponentSpec) this.specsByComponent.get(arrayList.get(elementIndex));
                    stringBuffer.append(lineSeparator + layoutComponentSpec.toDebugString());
                }

                this.reportError("Constraints could not be resolved: " + stringBuffer.toString());
            }
        }
    }

    public Dimension computeLayoutSize(Container container1, Integer integer, Integer integer1) {
        if (this.constraintLines == null) {
        }

        this.resetComponentSpecs();
        Insets insets = container1.getInsets();
        int integerValue = -1;
        int integer1Value = -1;
        if (integer != null) {
            integerValue = integer;
        }

        if (integer1 != null) {
            integer1Value = integer1;
        }

        int componentCountValue;
        if (integerValue != -1) {
            if (integer1Value != -1) {
                return new Dimension(integerValue + insets.left + insets.right, integer1Value + insets.top + insets.bottom);
            }

            componentCountValue = container1.getComponentCount();
        } else {
            componentCountValue = container1.getComponentCount();
        }

        int componentCountValue2 = componentCountValue;
        ArrayList arrayList = new ArrayList(componentCountValue2);
        this.resolveComponents(container1, arrayList, false);

        boolean bl;
        do {
            bl = false;

            for (int elementIndex = 0; elementIndex < arrayList.size(); elementIndex++) {
                Component component1 = (Component) arrayList.get(elementIndex);
                LayoutComponentSpec layoutComponentSpec = (LayoutComponentSpec) this.specsByComponent.get(component1);
                bl = ((layoutComponentSpec.resolvePreferredSize()) || (bl));
            }
        } while (bl);

        int defaultRightPadValue = 0;
        if (this.defaultRightPad != null) {
            defaultRightPadValue = this.defaultRightPad;
        }

        int defaultBottomPadValue = 0;
        if (this.defaultBottomPad != null) {
            defaultBottomPadValue = this.defaultBottomPad;
        }

        int integerValue2 = 0;
        int integer1Value2 = 0;

        for (int i = 0; i < componentCountValue2; i++) {
            Component component2 = container1.getComponent(i);
            LayoutComponentSpec layoutComponentSpec1 = (LayoutComponentSpec) this.specsByComponent.get(component2);
            if (!layoutComponentSpec1.isBoundsResolved()) {
                this.reportError("Constraints could not be resolved while determining preferred size. '" + layoutComponentSpec1 + "'");
            }

            Point point = layoutComponentSpec1.getBottomRight();
            point.x = point.x + Math.max(layoutComponentSpec1.getRightPad(), defaultRightPadValue);
            point.y = point.y + Math.max(layoutComponentSpec1.getBottomPad(), defaultBottomPadValue);
            if (point.x > integerValue2) {
                integerValue2 = point.x;
            }

            if (point.y > integer1Value2) {
                integer1Value2 = point.y;
            }
        }

        if (integerValue == -1) {
            integerValue = integerValue2;
        }

        if (integer1Value == -1) {
            integer1Value = integer1Value2;
        }

        if (this.minWidth != null) {
            integerValue = Math.max(integerValue, this.minWidth);
        }

        if (this.minHeight != null) {
            integer1Value = Math.max(integer1Value, this.minHeight);
        }

        return new Dimension(integerValue + insets.left + insets.right, integer1Value + insets.top + insets.bottom);
    }

    @Override
    public float getLayoutAlignmentX(Container container1) {
        return 0.5F;
    }

    public String setLayoutProperty(String string, String expressionText) {
        int ba;
        try {
            ba = Integer.parseInt(expressionText);
        } catch (NumberFormatException numberFormatException) {
            if (string.equals("width")) {
                this.layoutSizeExpressions[0] = new LayoutExpression(this, expressionText);
            } else if (string.equals("height")) {
                this.layoutSizeExpressions[1] = new LayoutExpression(this, expressionText);
            } else if (string.equals("minWidth")) {
                this.layoutSizeExpressions[2] = new LayoutExpression(this, expressionText);
            } else {
                if (!string.equals("minHeight")) {
                    return "Property '" + string + "' is not valid for the '" + "layout" + "' object.";
                }

                this.layoutSizeExpressions[3] = new LayoutExpression(this, expressionText);
            }

            return null;
        }

        if (string.equals("width")) {
            this.layoutWidth = ba;
        } else if (string.equals("height")) {
            this.layoutHeight = ba;
        } else if (string.equals("minWidth")) {
            this.minWidth = ba;
        } else {
            if (!string.equals("minHeight")) {
                return "Property '" + string + "' is not valid for the '" + "layout" + "' object.";
            }

            this.minHeight = ba;
        }

        return null;
    }

    public static int getPropertyIndex(String string) {
        if (string.equals("width")) {
            return 0;
        } else if (string.equals("height")) {
            return 1;
        } else if (string.equals("top")) {
            return 2;
        } else if (string.equals("bottom")) {
            return 4;
        } else if (string.equals("left")) {
            return 3;
        } else if (string.equals("right")) {
            return 5;
        } else if (string.equals("centerX")) {
            return 6;
        } else if (string.equals("centerY")) {
            return 7;
        } else if (string.equals("minWidth")) {
            return 8;
        } else if (string.equals("minHeight")) {
            return 9;
        } else if (string.equals("defaultWidth")) {
            return 10;
        } else {
            return string.equals("defaultHeight") ? 11 : -1;
        }
    }

    public String getVersion() {
        return "1.1";
    }

    public Dimension getAvailableSize() {
        Insets insets = this.container.getInsets();
        Dimension dimension = this.container.getSize();
        dimension.width = dimension.width - (insets.left + insets.left);
        dimension.height = dimension.height - (insets.top + insets.bottom);
        if (this.minWidth != null) {
            dimension.width = Math.max(dimension.width, this.minWidth);
        }

        if (this.minHeight != null) {
            dimension.height = Math.max(dimension.height, this.minHeight);
        }

        return dimension;
    }

    @Override
    public void invalidateLayout(Container container1) {
        this.resetComponentSpecs();
    }

    public int getContainerValue(int ba) {
        Dimension dimension = this.getAvailableSize();
        switch (ba) {
            case 0:
            case 5:
                return dimension.width;
            case 1:
            case 4:
                return dimension.height;
            case 2:
            case 3:
                return 0;
            case 6:
                return dimension.width / 2;
            case 7:
                return dimension.height / 2;
            default:
                this.reportError("'container." + getPropertyName(ba) + "' is not a valid RHS property.");
                return -1;
        }
    }

    public static int[] getControlFlags() {
        return controlFlags;
    }

    public ConstraintLayout(Container container1) {
        this.container = container1;
    }

    public LayoutComponentSpec getComponentSpec(String key) {
        Object keyValue = this.componentsByName.get(key);
        Map map1;
        if (keyValue == null) {
            this.reportError("Component '" + key + "' has not been added to the container.");
            map1 = this.specsByComponent;
        } else {
            map1 = this.specsByComponent;
        }

        return (LayoutComponentSpec) map1.get(keyValue);
    }

    public void resolveLayoutSizes() {
        if (!this.layoutSizesResolved) {
            int layoutSizeExpressionIndex = 0;
            int layoutSizeExpressionIndexValue = layoutSizeExpressionIndex;

            for (LayoutExpression[] layoutExpressions = this.layoutSizeExpressions; layoutSizeExpressionIndexValue < layoutExpressions.length; layoutExpressions = this.layoutSizeExpressions) {
                if (this.layoutSizeExpressions[layoutSizeExpressionIndex] != null && this.layoutSizeExpressions[layoutSizeExpressionIndex].tryEvaluate(false)) {
                    switch (layoutSizeExpressionIndex) {
                        case 0:
                            this.layoutWidth = this.layoutSizeExpressions[layoutSizeExpressionIndex].getValue();
                            break;
                        case 1:
                            this.layoutHeight = this.layoutSizeExpressions[layoutSizeExpressionIndex].getValue();
                            break;
                        case 2:
                            this.minWidth = this.layoutSizeExpressions[layoutSizeExpressionIndex].getValue();
                            break;
                        case 3:
                            this.minHeight = this.layoutSizeExpressions[layoutSizeExpressionIndex].getValue();
                    }
                }

                layoutSizeExpressionIndexValue = ++layoutSizeExpressionIndex;
            }

            this.layoutSizesResolved = true;
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container container1) {
        synchronized (container1.getTreeLock()) {
            this.resolveLayoutSizes();
            return this.computeLayoutSize(container1, this.layoutWidth, this.layoutHeight);
        }
    }

    public void reportError(String string) {
        String string1 = "PowerLayout " + this.getVersion() + " ERROR: " + lineSeparator + string;
        System.err.println(lineSeparator + string1);
        System.exit(1);
        throw new RuntimeException(string1);
    }

    @Override
    public Dimension minimumLayoutSize(Container container1) {
        synchronized (container1.getTreeLock()) {
            this.resolveLayoutSizes();
            Integer integer = this.minWidth != null ? this.minWidth : this.layoutWidth;
            Integer integer1 = this.minHeight != null ? this.minHeight : this.layoutHeight;
            return this.computeLayoutSize(container1, integer, integer1);
        }
    }

    public void resetComponentSpecs() {
        Iterator iterator = this.specsByComponent.values().iterator();

        while (iterator.hasNext()) {
            ((LayoutComponentSpec) iterator.next()).reset();
        }

        this.resolvePassStarted = false;
    }
}
