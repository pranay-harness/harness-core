package io.harness.pms.expressions;

import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.Set;

public class PMSExpressionEvaluatorProvider implements ExpressionEvaluatorProvider {
  @Override
  public EngineExpressionEvaluator get(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    return new PMSExpressionEvaluator(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }
}
