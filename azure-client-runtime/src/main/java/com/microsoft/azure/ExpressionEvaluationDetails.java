/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ExpressionEvaluationDetails {

    private String expression;

    private String expressionValue;

    private String operator;

    private String path;

    private String result;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> targetValue;

    public String getExpression() {
        return expression;
    }

    public String getExpressionValue() {
        return expressionValue;
    }

    public String getOperator() {
        return operator;
    }

    public String getPath() {
        return path;
    }

    public String getResult() {
        return result;
    }

    public List<String> getTargetValue() {
        return targetValue;
    }

    @Override
    public String toString() {
        return "ExpressionEvaluationDetails{"
                + "expression='" + expression + '\''
                + ", expressionValue='" + expressionValue + '\''
                + ", operator='" + operator + '\''
                + ", path='" + path + '\''
                + ", result='" + result + '\''
                + ", targetValue=" + targetValue
                + '}';
    }
}
