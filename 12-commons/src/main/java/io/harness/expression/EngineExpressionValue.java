package io.harness.expression;

/**
 * If a value returned after evaluating an expression implements EngineExpressionValue, instead of returning that object
 * we return object.fetchConcreteValue().
 */
public interface EngineExpressionValue { Object fetchConcreteValue(); }
