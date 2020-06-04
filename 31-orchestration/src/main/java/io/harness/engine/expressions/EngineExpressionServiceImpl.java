package io.harness.engine.expressions;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.ambiance.Ambiance;

public class EngineExpressionServiceImpl implements EngineExpressionService {
  @Inject private Injector injector;

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    EngineAmbianceExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    injector.injectMembers(evaluator);
    return evaluator.renderExpression(expression);
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    EngineAmbianceExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    injector.injectMembers(evaluator);
    return evaluator.evaluateExpression(expression);
  }

  @Override
  public Object resolve(Ambiance ambiance, Object o) {
    EngineAmbianceExpressionEvaluator evaluator = prepareExpressionEvaluator(ambiance);
    injector.injectMembers(evaluator);
    return evaluator.resolve(o);
  }

  private EngineAmbianceExpressionEvaluator prepareExpressionEvaluator(Ambiance ambiance) {
    return EngineAmbianceExpressionEvaluator.builder().ambiance(ambiance).refObjectSpecific(false).build();
  }
}
