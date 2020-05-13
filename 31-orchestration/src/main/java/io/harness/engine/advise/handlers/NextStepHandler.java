package io.harness.engine.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.adviser.NextStepAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.advise.AdviseHandler;
import io.harness.plan.ExecutionNode;

@OwnedBy(CDC)
public class NextStepHandler implements AdviseHandler<NextStepAdvise> {
  @Inject private ExecutionEngine engine;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  @Override
  public void handleAdvise(Ambiance ambiance, NextStepAdvise advise) {
    ExecutionNode nextNode = Preconditions.checkNotNull(
        engineObtainmentHelper.fetchExecutionNode(advise.getNextNodeId(), ambiance.getPlanExecutionId()));
    engine.triggerExecution(ambiance, nextNode);
  }
}
