package io.harness.pms.expression;

import io.harness.pms.contracts.ambiance.Ambiance;

public interface PmsEngineExpressionService {
  String renderExpression(Ambiance ambiance, String expression);
  String evaluateExpression(Ambiance ambiance, String expression);
  Object resolve(Ambiance ambiance, Object o);
}
