package com.zelix.klassmaster.ui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class LayoutExpression implements LayoutExpressionNode, LayoutConstraintMarker {
    public boolean resolved;
    public int value;
    public boolean parsed;
    public List operands = new ArrayList();
    public List operators = new ArrayList();
    public ConstraintLayout layout;
    public String expressionText;

    public boolean isOperator(String string) {
        return string.equals("+") || string.equals("-") || string.equals("*") || string.equals("/");
    }

    @Override
    public boolean tryEvaluate(Object object) {
        boolean bl = (Boolean) object;
        if (!this.resolved) {
            this.resolved = true;
            int elementIndex = 0;
            int bc = 0;

            for (List list1 = this.operands; bc < list1.size(); list1 = this.operands) {
                int valueValue = 0;
                LayoutExpressionNode layoutExpressionNode = (LayoutExpressionNode) this.operands.get(elementIndex);
                if (layoutExpressionNode.tryEvaluate(bl)) {
                    valueValue = layoutExpressionNode.getValue();
                } else {
                    this.resolved = false;
                }

                if (this.resolved) {
                    if (elementIndex == 0) {
                        this.value = valueValue;
                    } else {
                        String string = (String) this.operators.get(elementIndex - 1);
                        this.value = this.applyOperator(this.value, valueValue, string);
                    }
                }

                bc = ++elementIndex;
            }
        }

        return this.resolved;
    }

    @Override
    public boolean isContainerRelative() {
        return ((LayoutExpressionNode) this.operands.get(0)).isContainerRelative();
    }

    public int getPaddingValue() {
        int ba = 0;
        int elementIndex = 1;
        int bd = 1;

        for (List list1 = this.operands; bd < list1.size(); list1 = this.operands) {
            LayoutExpressionNode layoutExpressionNode = (LayoutExpressionNode) this.operands.get(elementIndex);
            if (layoutExpressionNode.tryEvaluate(false)) {
                int value = layoutExpressionNode.getValue();
                String string = (String) this.operators.get(elementIndex - 1);
                ba = this.applyOperator(ba, value, string);
            }

            bd = ++elementIndex;
        }

        return ba * -1;
    }

    @Override
    public void reset() {
        int elementIndex = 0;
        int bb = 0;

        for (List list1 = this.operands; bb < list1.size(); list1 = this.operands) {
            ((LayoutExpressionNode) this.operands.get(elementIndex)).reset();
            bb = ++elementIndex;
        }

        this.resolved = false;
        this.value = Integer.MIN_VALUE;
    }

    public String parseExpression() {
        this.parsed = true;
        StringTokenizer stringTokenizer = new StringTokenizer(this.expressionText, "+-*/", true);

        while (stringTokenizer.hasMoreTokens()) {
            String key = stringTokenizer.nextToken().trim();
            if (this.isOperator(key)) {
                this.operators.add(key);
            } else {
                int endIndex = key.indexOf(".");
                LayoutExpressionNode layoutExpressionNode;
                if (LayoutFunctionTerm.isFunctionCall(key)) {
                    LayoutFunctionTerm layoutFunctionTerm = new LayoutFunctionTerm(this.layout, key);
                    String string1 = layoutFunctionTerm.prepare();
                    if (string1 != null) {
                        return string1;
                    }

                    layoutExpressionNode = layoutFunctionTerm;
                } else if (endIndex != -1) {
                    String keyValue = key.substring(0, endIndex).trim();
                    String string3 = key.substring(endIndex + 1).trim();
                    int propertyIndex = ConstraintLayout.getPropertyIndex(string3);
                    if (keyValue.equals("container")) {
                        layoutExpressionNode = new LayoutConstantTerm(this.layout, propertyIndex);
                    } else {
                        LayoutComponentSpec layoutComponentSpec = this.layout.getComponentSpec(keyValue);
                        layoutExpressionNode = new LayoutConstraintRef(layoutComponentSpec, propertyIndex);
                    }
                } else if (Character.isDigit(key.charAt(0))) {
                    try {
                        int valueValue = Integer.parseInt(key);
                        layoutExpressionNode = new LayoutNumberTerm(valueValue);
                    } catch (NumberFormatException numberFormatException) {
                        return "Parse error '" + this.expressionText + "'";
                    }
                } else {
                    LayoutExpression layoutExpression1 = this.layout.getVariable(key);
                    String string4 = layoutExpression1.prepare();
                    if (string4 != null) {
                        return string4;
                    }

                    layoutExpressionNode = layoutExpression1;
                }

                this.operands.add(layoutExpressionNode);
                if (this.operands.size() > 1 && layoutExpressionNode.isContainerRelative()) {
                    return "Parse error. Only the first operand can be container relative : '" + key + "' in '" + this.expressionText + "'";
                }
            }
        }

        return null;
    }

    public LayoutExpression(ConstraintLayout constraintLayout1, String expressionTextArgument) {
        this.layout = constraintLayout1;
        this.expressionText = expressionTextArgument;
    }

    @Override
    public boolean isContainerSizeRelative() {
        return ((LayoutExpressionNode) this.operands.get(0)).isContainerSizeRelative();
    }

    public int applyOperator(int ba, int bb, String string) {
        if (string.equals("+")) {
            return ba + bb;
        } else if (string.equals("-")) {
            return ba - bb;
        } else {
            return string.equals("*") ? ba * bb : ba / bb;
        }
    }

    public String getExpressionText() {
        return this.expressionText;
    }

    public boolean isResolved() {
        return this.resolved;
    }

    public int getContainerPropertyIndex() {
        LayoutExpressionNode layoutExpressionNode = (LayoutExpressionNode) this.operands.get(0);
        return layoutExpressionNode instanceof LayoutConstantTerm ? ((LayoutConstantTerm) layoutExpressionNode).getPropertyIndex() : 0;
    }

    @Override
    public boolean isContainerLeadingEdge() {
        return ((LayoutExpressionNode) this.operands.get(0)).isContainerLeadingEdge();
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public boolean isContainerTrailingEdge() {
        return ((LayoutExpressionNode) this.operands.get(0)).isContainerTrailingEdge();
    }

    @Override
    public String prepare() {
        this.reset();
        return !this.parsed ? this.parseExpression() : null;
    }

    static {
        try {
            staticInit();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void staticInit() {
    }
}
