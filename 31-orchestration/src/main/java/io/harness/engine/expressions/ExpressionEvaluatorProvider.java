package io.harness.engine.expressions;

import io.harness.ambiance.Ambiance;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;

import java.util.Set;

public interface ExpressionEvaluatorProvider {
  EngineExpressionEvaluator get(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific);
}
