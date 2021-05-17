package io.harness.pms.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;

@OwnedBy(HarnessTeam.PIPELINE)
public interface EngineExpressionService {
  String renderExpression(Ambiance ambiance, String expression);
  String renderExpression(Ambiance ambiance, String expression, boolean skipUnresolvedExpressionsCheck);

  Object evaluateExpression(Ambiance ambiance, String expression);
  Object resolve(Ambiance ambiance, Object o, boolean skipUnresolvedExpressionsCheck);

  EngineExpressionEvaluator prepareExpressionEvaluator(Ambiance ambiance);
}
