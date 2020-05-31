package io.harness.engine.expressions;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.ambiance.Ambiance;
import io.harness.expression.VariableResolverTracker;

public class EngineExpressionServiceImpl implements EngineExpressionService {
  @Inject private Injector injector;

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    EngineAmbianceExpressionEvaluator evaluator = EngineAmbianceExpressionEvaluator.builder()
                                                      .ambiance(ambiance)
                                                      .variableResolverTracker(new VariableResolverTracker())
                                                      .build();
    injector.injectMembers(evaluator);
    return evaluator.renderExpression(expression);
  }

  @Override
  public Object evaluateExpression(Ambiance ambiance, String expression) {
    EngineAmbianceExpressionEvaluator evaluator = EngineAmbianceExpressionEvaluator.builder()
                                                      .ambiance(ambiance)
                                                      .variableResolverTracker(new VariableResolverTracker())
                                                      .build();
    injector.injectMembers(evaluator);
    return evaluator.evaluateExpression(expression);
  }

  @Override
  public Object resolve(Ambiance ambiance, Object o) {
    EngineAmbianceExpressionEvaluator evaluator = EngineAmbianceExpressionEvaluator.builder()
                                                      .ambiance(ambiance)
                                                      .variableResolverTracker(new VariableResolverTracker())
                                                      .build();
    injector.injectMembers(evaluator);
    return evaluator.resolve(o);
  }
}
