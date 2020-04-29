package io.harness.engine;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.persistence.HPersistence;
import io.harness.state.execution.NodeExecution;
import io.harness.state.execution.NodeExecution.NodeExecutionKeys;
import io.harness.state.execution.PlanExecution;
import io.harness.state.execution.PlanExecution.PlanExecutionKeys;

@Redesign
public class AmbianceHelper {
  @Inject private HPersistence hPersistence;

  public NodeExecution obtainNodeExecution(Ambiance ambiance) {
    String nodeInstanceId = ambiance.obtainCurrentRuntimeId();
    if (nodeInstanceId == null) {
      return null;
    }
    return hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeInstanceId).get();
  }

  public PlanExecution obtainExecutionInstance(Ambiance ambiance) {
    String executionId = ambiance.getExecutionInstanceId();
    return hPersistence.createQuery(PlanExecution.class).filter(PlanExecutionKeys.uuid, executionId).get();
  }
}
