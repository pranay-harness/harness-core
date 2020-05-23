package io.harness.engine.expressions;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.ExcludeRedesign;
import io.harness.engine.expressions.functors.OutcomeFunctor;
import io.harness.engine.services.OutcomeService;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.JsonFunctor;
import io.harness.expression.LateBindingMap;
import io.harness.expression.RegexFunctor;
import io.harness.expression.VariableResolverTracker;
import io.harness.expression.XmlFunctor;
import io.harness.reflection.ReflectionUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Redesign
@ExcludeRedesign
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

  /**
   * This method renders expressions recursively for all String fields inside the given object. If a field is annotated
   * with @NotExpression, it is skipped.
   *
   * @param o the object to resolve
   * @return the resolved object (this can be the same object or a new one)
   */
  public Object resolve(Object o) {
    // If there is such a field, mark is with @NotExpression.
    return ReflectionUtils.updateStrings(o, f -> f.isAnnotationPresent(NotExpression.class), this ::renderExpression);
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
