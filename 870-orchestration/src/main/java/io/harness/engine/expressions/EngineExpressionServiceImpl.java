package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDC)
@Singleton
public class EngineExpressionServiceImpl implements EngineExpressionService {
  @Inject PmsEngineExpressionService pmsEngineExpressionService;

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    return pmsEngineExpressionService.renderExpression(ambiance, expression);
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    String json = pmsEngineExpressionService.evaluateExpression(ambiance, expression);
    Object result;
    try {
      result = RecastOrchestrationUtils.fromDocumentJson(json, Object.class);
    } catch (Exception e) {
      result = json;
    }
    return result;
  }

  @Override
  public Object resolve(Ambiance ambiance, Object o) {
    return pmsEngineExpressionService.resolve(ambiance, o);
  }
}
