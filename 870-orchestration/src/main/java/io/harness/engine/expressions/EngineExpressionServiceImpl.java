package io.harness.engine.expressions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class EngineExpressionServiceImpl implements EngineExpressionService {
  @Inject PmsEngineExpressionService pmsEngineExpressionService;

  @Override
  public String renderExpression(Ambiance ambiance, String expression, boolean skipUnresolvedExpressionsCheck) {
    return pmsEngineExpressionService.renderExpression(ambiance, expression, skipUnresolvedExpressionsCheck);
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    String json = pmsEngineExpressionService.evaluateExpression(ambiance, expression);
    Object result;
    try {
      result = RecastOrchestrationUtils.fromJson(json, Object.class);
    } catch (Exception e) {
      result = json;
    }
    return result;
  }
}
