package io.harness.engine.expressions;

import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.ambiance.Ambiance;

import java.util.Set;

public class SampleExpressionEvaluatorProvider implements ExpressionEvaluatorProvider {
  private final boolean supportStringUtils;

  public SampleExpressionEvaluatorProvider(boolean supportStringUtils) {
    this.supportStringUtils = supportStringUtils;
  }

  @Override
  public EngineExpressionEvaluator get(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    return new SampleExpressionEvaluator(variableResolverTracker, supportStringUtils);
  }
}
