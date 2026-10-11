package com.zelix.klassmaster.ui.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

public class LayoutComponentSpec implements LayoutConstraintMarker {
    public Point cachedLocation;
    public int bottomPad;
    public Dimension cachedSize;
    public int rightPad;
    public LayoutExpression[] preferredSizeExpressions;
    public LayoutExpression[] expressions = new LayoutExpression[10];
    public Integer[] values = new Integer[10];
    public String componentName;
    public Component component;
    public ConstraintLayout layout;

    public int getBottomPad() {
        return this.bottomPad;
    }

    public boolean isBoundsResolved() {
        return this.values[0] != null && this.values[1] != null && this.values[3] != null && this.values[2] != null;
    }

    public boolean areExpressionsResolved(boolean bl) {
        int expressionIndex = 0;
        int bb = 0;

        for (byte bc = 10; bb < bc; bc = 10) {
            LayoutExpression layoutExpression = this.expressions[expressionIndex];
            if (layoutExpression != null && !layoutExpression.tryEvaluate(bl)) {
                return false;
            }

            bb = ++expressionIndex;
        }

        return true;
    }

    public void evaluateProperty(int ba, LayoutExpression[] layoutExpressions, boolean[] booleanArray, boolean bl1) {
        if (this.values[ba] == null) {
            LayoutExpression layoutExpression = layoutExpressions[ba];
            if (layoutExpression != null && layoutExpression.tryEvaluate(bl1)) {
                this.values[ba] = layoutExpression.getValue();
                booleanArray[0] = true;
            }
        }
    }

    public int getRightPad() {
        return this.rightPad;
    }

    public void reset() {
        this.cachedSize = null;
        this.cachedLocation = null;
        int ba = 0;
        int bc = 0;

        for (byte bd = 10; bc < bd; bd = 10) {
            this.values[ba] = null;
            bc = ++ba;
        }

        ba = 0;
        bc = 0;

        for (byte bb = 10; bc < bb; bb = 10) {
            LayoutExpression layoutExpression = this.expressions[ba];
            if (layoutExpression != null) {
                layoutExpression.reset();
            }

            bc = ++ba;
        }
    }

    public String toDebugString() {
        StringBuffer stringBuffer = new StringBuffer(this.componentName + " : {");
        int ba = 0;
        int bc = 0;

        for (byte bd = 10; bc < bd; bd = 10) {
            LayoutExpression layoutExpression = this.expressions[ba];
            if (layoutExpression != null) {
                stringBuffer.append(" " + ConstraintLayout.getPropertyName(ba) + "=" + layoutExpression.getExpressionText());
            }

            bc = ++ba;
        }

        stringBuffer.append("} {");
        ba = 0;
        bc = 0;

        for (byte bb = 10; bc < bb; bb = 10) {
            stringBuffer.append(" " + ConstraintLayout.getPropertyName(ba) + "=" + this.values[ba]);
            bc = ++ba;
        }

        stringBuffer.append("}");
        return stringBuffer.toString();
    }

    public String getComponentName() {
        return this.componentName;
    }

    public LayoutComponentSpec(String componentNameArgument, Component component1, ConstraintLayout constraintLayout1) {
        this.componentName = componentNameArgument;
        this.component = component1;
        this.layout = constraintLayout1;
    }

    public int getPropertyValue(int valueIndex) {
        if (valueIndex == 10) {
            return this.component.getPreferredSize().width;
        } else {
            return valueIndex == 11 ? this.component.getPreferredSize().height : this.values[valueIndex];
        }
    }

    public Point getBottomRight() {
        return new Point(this.values[5], this.values[4]);
    }

    public boolean isPropertyResolved(int valueIndex) {
        return ((valueIndex == 10 || valueIndex == 11) || (this.values[valueIndex] != null));
    }

    public void deriveProperty(int ba, LayoutExpression[] layoutExpressions, boolean[] booleanArray) {
        if (layoutExpressions[ba] == null && this.values[ba] == null) {
            int value = Integer.MIN_VALUE;
            switch (ba) {
                case 0:
                    if (layoutExpressions[3] != null && layoutExpressions[5] != null) {
                        if (this.values[3] != null && this.values[5] != null) {
                            value = this.values[5] - this.values[3];
                        }
                    } else if (layoutExpressions[3] != null && layoutExpressions[6] != null) {
                        if (this.values[3] != null && this.values[6] != null) {
                            value = (this.values[6] - this.values[3]) * 2;
                        }
                    } else {
                        Component component1;
                        if (layoutExpressions[6] != null) {
                            if (layoutExpressions[5] != null) {
                                if (this.values[6] != null && this.values[5] != null) {
                                    value = (this.values[5] - this.values[6]) * 2;
                                }
                                break;
                            }

                            component1 = this.component;
                        } else {
                            component1 = this.component;
                        }

                        value = component1.getPreferredSize().width;
                        LayoutExpression layoutExpression1 = this.expressions[8];
                        if (layoutExpression1 != null) {
                            value = Math.max(value, layoutExpression1.getValue());
                        }
                    }
                    break;
                case 1:
                    if (layoutExpressions[4] != null && layoutExpressions[2] != null) {
                        if (this.values[4] != null && this.values[2] != null) {
                            value = this.values[4] - this.values[2];
                        }
                    } else if (layoutExpressions[2] != null && layoutExpressions[7] != null) {
                        if (this.values[7] != null && this.values[2] != null) {
                            value = (this.values[7] - this.values[2]) * 2;
                        }
                    } else {
                        Component component2;
                        if (layoutExpressions[7] != null) {
                            if (layoutExpressions[4] != null) {
                                if (this.values[7] != null && this.values[4] != null) {
                                    value = (this.values[4] - this.values[7]) * 2;
                                }
                                break;
                            }

                            component2 = this.component;
                        } else {
                            component2 = this.component;
                        }

                        value = component2.getPreferredSize().height;
                        LayoutExpression layoutExpression = this.expressions[9];
                        if (layoutExpression != null) {
                            value = Math.max(value, layoutExpression.getValue());
                        }
                    }
                    break;
                case 2:
                    if (layoutExpressions[7] == null && layoutExpressions[4] == null) {
                        value = 0;
                    } else {
                        if (this.values[1] == null) {
                            return;
                        }

                        int valueValue = this.values[1];
                        if (this.values[4] != null) {
                            value = this.values[4] - valueValue;
                        } else if (this.values[7] != null) {
                            value = this.values[7] - valueValue / 2;
                        }
                    }
                    break;
                case 3:
                    if (layoutExpressions[6] == null && layoutExpressions[5] == null) {
                        value = 0;
                    } else {
                        if (this.values[0] == null) {
                            return;
                        }

                        int valueValue2 = this.values[0];
                        if (this.values[5] != null) {
                            value = this.values[5] - valueValue2;
                        } else if (this.values[6] != null) {
                            value = this.values[6] - valueValue2 / 2;
                        }
                    }
                    break;
                case 4:
                    if (this.values[1] == null) {
                        return;
                    }

                    int valueValue3 = this.values[1];
                    if (this.values[2] != null) {
                        value = this.values[2] + valueValue3;
                    } else if (this.values[7] != null) {
                        value = this.values[7] + valueValue3 / 2;
                    }
                    break;
                case 5:
                    if (this.values[0] == null) {
                        return;
                    }

                    int valueValue4 = this.values[0];
                    if (this.values[3] != null) {
                        value = this.values[3] + valueValue4;
                    } else if (this.values[6] != null) {
                        value = this.values[6] + valueValue4 / 2;
                    }
                    break;
                case 6:
                    if (this.values[0] == null) {
                        return;
                    }

                    int valueValue5 = this.values[0];
                    if (this.values[3] != null) {
                        value = this.values[3] + valueValue5 / 2;
                    } else if (this.values[5] != null) {
                        value = this.values[5] - valueValue5 / 2;
                    }
                    break;
                case 7:
                    if (this.values[1] == null) {
                        return;
                    }

                    int valueValue6 = this.values[1];
                    if (this.values[2] != null) {
                        value = this.values[2] + valueValue6 / 2;
                    } else if (this.values[4] != null) {
                        value = this.values[4] - valueValue6 / 2;
                    }
            }

            if (value != Integer.MIN_VALUE) {
                booleanArray[0] = true;
                this.values[ba] = value;
            }
        }
    }

    public Point getLocation() {
        return new Point(this.values[3], this.values[2]);
    }

    public String validateExpressions() {
        this.cachedSize = null;
        this.cachedLocation = null;
        int expressionIndex = 0;
        int bb = 0;

        for (byte bc = 10; bb < bc; bc = 10) {
            LayoutExpression layoutExpression = this.expressions[expressionIndex];
            if (layoutExpression != null) {
                String string = layoutExpression.prepare();
                if (string != null) {
                    return string;
                }
            }

            bb = ++expressionIndex;
        }

        return null;
    }

    public boolean resolveProperties(boolean bl, LayoutExpression[] layoutExpressions) {
        boolean[] booleanArray = new boolean[]{false};
        int ba = 0;
        int bd = 0;

        for (byte be = 10; bd < be; be = 10) {
            this.evaluateProperty(ba, layoutExpressions, booleanArray, bl);
            bd = ++ba;
        }

        boolean bl4 = false;

        for (boolean booleanArrayValueElement = true; booleanArrayValueElement; bl4 = ((booleanArrayValueElement) || (bl4))) {
            boolean[] booleanArrayValue = new boolean[]{false};
            int bb = 0;
            bd = 0;

            for (byte bc = 8; bd < bc; bc = 8) {
                this.deriveProperty(bb, layoutExpressions, booleanArrayValue);
                bd = ++bb;
            }

            booleanArrayValueElement = booleanArrayValue[0];
        }

        return booleanArray[0] || bl4;
    }

    public String setProperty(String string, String expressionText) {
        int propertyIndex = ConstraintLayout.getPropertyIndex(string);
        this.expressions[propertyIndex] = new LayoutExpression(this.layout, expressionText);
        return null;
    }

    public boolean resolve(boolean bl) {
        return this.resolveProperties(bl, this.expressions);
    }

    public Dimension getSize() {
        return new Dimension(this.values[0], this.values[1]);
    }

    public boolean resolvePreferredSize() {
        if (this.preferredSizeExpressions == null) {
            this.preferredSizeExpressions = new LayoutExpression[10];
            System.arraycopy(this.expressions, 0, this.preferredSizeExpressions, 0, 10);
            int preferredSizeExpressionIndex = 0;
            int bb = 0;

            for (byte bc = 10; bb < bc; bc = 10) {
                LayoutExpression layoutExpression = this.preferredSizeExpressions[preferredSizeExpressionIndex];
                if (layoutExpression != null && !layoutExpression.isResolved()) {
                    if (layoutExpression.isContainerSizeRelative()) {
                        this.preferredSizeExpressions[preferredSizeExpressionIndex] = null;
                    } else if (layoutExpression.isContainerTrailingEdge()) {
                        if (layoutExpression.getContainerPropertyIndex() == 5) {
                            this.rightPad = layoutExpression.getPaddingValue();
                        } else {
                            this.bottomPad = layoutExpression.getPaddingValue();
                        }

                        this.preferredSizeExpressions[preferredSizeExpressionIndex] = null;
                    } else if (layoutExpression.isContainerLeadingEdge()) {
                        layoutExpression.tryEvaluate(true);
                    }
                }

                bb = ++preferredSizeExpressionIndex;
            }
        }

        boolean bl = false;

        while (this.resolveProperties(false, this.preferredSizeExpressions)) {
            bl = true;
        }

        return bl;
    }
}
