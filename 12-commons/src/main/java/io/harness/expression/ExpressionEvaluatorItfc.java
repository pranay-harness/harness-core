package io.harness.expression;

import org.apache.commons.jexl3.JexlContext;

public interface ExpressionEvaluatorItfc { Object evaluate(String expression, JexlContext context); }
