package io.harness.engine.expresssions;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.engine.expresssions.functors.OutcomeFunctor;
import io.harness.engine.services.OutcomeService;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.JsonFunctor;
import io.harness.expression.LateBindingMap;
import io.harness.expression.RegexFunctor;
import io.harness.expression.VariableResolverTracker;
import io.harness.expression.XmlFunctor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
public class EngineExpressionEvaluator extends ExpressionEvaluator {
  VariableResolverTracker variableResolverTracker;
  Ambiance ambiance;

  private transient Map<String, Object> contextMap;
  @Inject private OutcomeService outcomeService;

  @Builder
  public EngineExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance) {
    addFunctor("regex", new RegexFunctor());
    addFunctor("json", new JsonFunctor());
    addFunctor("xml", new XmlFunctor());
    this.variableResolverTracker = variableResolverTracker;
    this.ambiance = ambiance;
  }

  public String renderExpression(String expression) {
    Map<String, Object> context = prepareAmbianceContext();
    return renderExpression(expression, context);
  }

  private Map<String, Object> prepareAmbianceContext() {
    if (contextMap != null) {
      return contextMap;
    }
    contextMap = new LateBindingMap();
    contextMap.put("context", OutcomeFunctor.builder().ambiance(ambiance).outcomeService(outcomeService).build());
    return contextMap;
  }

  public String renderExpression(String expression, Map<String, Object> context) {
    return substitute(expression, context, variableResolverTracker);
  }
}
