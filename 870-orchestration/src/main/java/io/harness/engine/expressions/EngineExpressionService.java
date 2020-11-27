package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;

@OwnedBy(CDC)
public interface EngineExpressionService {
  String renderExpression(Ambiance ambiance, String expression);
  Object evaluateExpression(Ambiance ambiance, String expression);
  Object resolve(Ambiance ambiance, Object o);
}
