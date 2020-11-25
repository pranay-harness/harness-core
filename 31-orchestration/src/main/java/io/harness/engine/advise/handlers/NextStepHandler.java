package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.advise.NextStepAdvise;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviseHandler;
import io.harness.plan.PlanNode;
import io.harness.pms.ambiance.Ambiance;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@OwnedBy(CDC)
public class NextStepHandler implements AdviseHandler<NextStepAdvise> {
  @Inject private OrchestrationEngine engine;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  @Override
  public void handleAdvise(Ambiance ambiance, NextStepAdvise advise) {
    PlanNode nextNode = Preconditions.checkNotNull(
        engineObtainmentHelper.fetchExecutionNode(advise.getNextNodeId(), ambiance.getPlanExecutionId()));
    engine.triggerExecution(ambiance, nextNode);
  }
}
