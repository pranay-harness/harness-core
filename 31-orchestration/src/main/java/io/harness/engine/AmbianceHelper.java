package io.harness.engine;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.persistence.HPersistence;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.ExecutionNodeInstance.ExecutionNodeInstanceKeys;
import io.harness.state.execution.PlanExecution;
import io.harness.state.execution.PlanExecution.PlanExecutionKeys;

@Redesign
public class AmbianceHelper {
  @Inject private HPersistence hPersistence;

  public ExecutionNodeInstance obtainNodeInstance(Ambiance ambiance) {
    String nodeInstanceId = ambiance.obtainCurrentRuntimeId();
    if (nodeInstanceId == null) {
      return null;
    }
    return hPersistence.createQuery(ExecutionNodeInstance.class)
        .filter(ExecutionNodeInstanceKeys.uuid, nodeInstanceId)
        .get();
  }

  public PlanExecution obtainExecutionInstance(Ambiance ambiance) {
    String executionId = ambiance.getExecutionInstanceId();
    return hPersistence.createQuery(PlanExecution.class).filter(PlanExecutionKeys.uuid, executionId).get();
  }
}
