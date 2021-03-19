/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure;

import java.util.List;

public class EvaluationDetails {

    private List<ExpressionEvaluationDetails> evaluatedExpressions;

    public List<ExpressionEvaluationDetails> getEvaluatedExpressions() {
        return evaluatedExpressions;
    }

    @Override
    public String toString() {
        return "EvaluationDetails{"
                + "evaluatedExpressions=" + evaluatedExpressions
                + '}';
    }
}
