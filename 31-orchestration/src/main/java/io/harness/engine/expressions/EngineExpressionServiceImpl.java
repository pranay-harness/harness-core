package io.harness.engine.expressions;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.EngineExpressionEvaluator;

import com.google.inject.Inject;
import com.google.inject.Injector;

@OwnedBy(CDC)
public class EngineExpressionServiceImpl implements EngineExpressionService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    EngineExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    injector.injectMembers(evaluator);
    return evaluator.renderExpression(expression);
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    EngineExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    injector.injectMembers(evaluator);
    return evaluator.evaluateExpression(expression);
  }

  @Override
  public Object resolve(Ambiance ambiance, Object o) {
    EngineExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    injector.injectMembers(evaluator);
    return evaluator.resolve(o);
  }

  private EngineExpressionEvaluator prepareExpressionEvaluator(Ambiance ambiance) {
    return expressionEvaluatorProvider.get(null, ambiance, null, false);
  }
}
