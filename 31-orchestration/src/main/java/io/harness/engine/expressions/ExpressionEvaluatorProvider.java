package io.harness.engine.expressions;

import io.harness.ambiance.Ambiance;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;

import java.util.Set;

public interface ExpressionEvaluatorProvider {
  /**
   * Provides an instance of {@link EngineExpressionEvaluator}. Must never return {@code null}.
   *
   * @param variableResolverTracker used by EngineExpressionEvaluator constructor
   * @param ambiance                used by AmbianceExpressionEvaluator constructor
   * @param entityTypes             used by AmbianceExpressionEvaluator constructor
   * @param refObjectSpecific       used by AmbianceExpressionEvaluator constructor
   * @return a new instance of EngineExpressionEvaluator
   */
  EngineExpressionEvaluator get(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific);
}
