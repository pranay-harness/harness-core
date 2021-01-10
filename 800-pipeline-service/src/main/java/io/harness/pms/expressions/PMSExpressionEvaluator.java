package io.harness.pms.expressions;

import io.harness.engine.expressions.AmbianceExpressionEvaluator;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.expression.VariableResolverTracker;
import io.harness.ngpipeline.expressions.functors.EventPayloadFunctor;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.plan.execution.SetupAbstractionKeys;

import java.util.Set;

public class PMSExpressionEvaluator extends AmbianceExpressionEvaluator {
  public PMSExpressionEvaluator(VariableResolverTracker variableResolverTracker, Ambiance ambiance,
      Set<NodeExecutionEntityType> entityTypes, boolean refObjectSpecific) {
    super(variableResolverTracker, ambiance, entityTypes, refObjectSpecific);
  }

  @Override
  protected void initialize() {
    super.initialize();
    //    addToContext("account", new AccountFunctor(accountService, ambiance));
    addToContext(SetupAbstractionKeys.eventPayload, new EventPayloadFunctor(ambiance));
  }
}
