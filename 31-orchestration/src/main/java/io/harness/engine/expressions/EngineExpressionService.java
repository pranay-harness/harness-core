package io.harness.engine.expressions;

import io.harness.ambiance.Ambiance;

public interface EngineExpressionService {
  String renderExpression(Ambiance ambiance, String expression);
  Object evaluateExpression(Ambiance ambiance, String expression);
  Object resolve(Ambiance ambiance, Object o);
}
