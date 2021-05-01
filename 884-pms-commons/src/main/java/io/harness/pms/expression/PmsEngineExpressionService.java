package io.harness.pms.expression;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;

@OwnedBy(PIPELINE)
public interface PmsEngineExpressionService {
  String renderExpression(Ambiance ambiance, String expression);
  String evaluateExpression(Ambiance ambiance, String expression);
  Object resolve(Ambiance ambiance, Object o);
  default EngineExpressionEvaluator prepareExpressionEvaluator(Ambiance ambiance) {
    return null;
  }
}
