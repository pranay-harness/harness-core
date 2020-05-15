package io.harness.engine.expresssions;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.ambiance.Ambiance;
import io.harness.expression.VariableResolverTracker;

public class EngineExpressionServiceImpl implements EngineExpressionService {
  @Inject private Injector injector;

  @Override
  public String renderExpression(Ambiance ambiance, String expression) {
    EngineExpressionEvaluator evaluator = EngineExpressionEvaluator.builder()
                                              .ambiance(ambiance)
                                              .variableResolverTracker(new VariableResolverTracker())
                                              .build();
    injector.injectMembers(evaluator);
    return evaluator.renderExpression(expression);
  }
}
