package io.harness.engine.pms.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.expression.SdkExpressionService;

import com.google.inject.Inject;
import com.google.inject.Injector;

@OwnedBy(HarnessTeam.PIPELINE)
public class EngineExpressionServiceImpl implements EngineExpressionService, SdkExpressionService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    return renderExpression(ambiance, expression, false);
  }

  @Override
  public String renderExpression(Ambiance ambiance, String expression, boolean skipUnresolvedExpressionsCheck) {
    EngineExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    return evaluator.renderExpression(expression, skipUnresolvedExpressionsCheck);
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    EngineExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    return evaluator.evaluateExpression(expression);
  }

  @Override
  public Object resolve(Ambiance ambiance, Object o, boolean skipUnresolvedExpressionsCheck) {
    EngineExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    return evaluator.resolve(o, skipUnresolvedExpressionsCheck);
  }

  @Override
  public EngineExpressionEvaluator prepareExpressionEvaluator(Ambiance ambiance) {
    EngineExpressionEvaluator engineExpressionEvaluator = expressionEvaluatorProvider.get(null, ambiance, null, false);
    injector.injectMembers(engineExpressionEvaluator);
    return engineExpressionEvaluator;
  }
}
