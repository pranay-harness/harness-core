package io.harness.engine.advise.handlers;

import com.google.inject.Inject;

import io.harness.adviser.Advise;
import io.harness.adviser.impl.success.OnSuccessAdvise;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.advise.AdviseHandler;
import io.harness.plan.ExecutionNode;
import io.harness.state.io.ambiance.Ambiance;

public class OnSuccessHandler implements AdviseHandler {
  @Inject private ExecutionEngine engine;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  @Override
  public void handleAdvise(Ambiance ambiance, Advise advise) {
    OnSuccessAdvise onSuccessAdvise = (OnSuccessAdvise) advise;
    ExecutionNode nextNode = engineObtainmentHelper.fetchExecutionNode(
        onSuccessAdvise.getNextNodeId(), ambiance.getSetupAbstractions().get("executionInstanceId"));
    engine.triggerExecution(ambiance.cloneBuilder(), nextNode);
  }
}
