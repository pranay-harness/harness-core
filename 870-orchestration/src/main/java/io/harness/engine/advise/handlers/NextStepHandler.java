package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.advise.AdviserResponseHandler;
import io.harness.pms.advisers.AdviserResponse;
import io.harness.pms.advisers.NextStepAdvise;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.plan.PlanNodeProto;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@OwnedBy(CDC)
public class NextStepHandler implements AdviserResponseHandler {
  @Inject private OrchestrationEngine engine;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  @Override
  public void handleAdvise(Ambiance ambiance, AdviserResponse adviserResponse) {
    NextStepAdvise advise = adviserResponse.getNextStepAdvise();
    PlanNodeProto nextNode = Preconditions.checkNotNull(
        engineObtainmentHelper.fetchExecutionNode(advise.getNextNodeId(), ambiance.getPlanExecutionId()));
    engine.triggerExecution(ambiance, nextNode);
  }
}
