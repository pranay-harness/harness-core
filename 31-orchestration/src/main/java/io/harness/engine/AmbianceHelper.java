package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecution.PlanExecutionKeys;
import io.harness.persistence.HPersistence;

@OwnedBy(CDC)
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
    String executionId = ambiance.getPlanExecutionId();
    return hPersistence.createQuery(PlanExecution.class).filter(PlanExecutionKeys.uuid, executionId).get();
  }

  public Ambiance fetchAmbiance(NodeExecution nodeExecution) {
    PlanExecution planExecution = hPersistence.createQuery(PlanExecution.class)
                                      .filter(PlanExecutionKeys.uuid, nodeExecution.getPlanExecutionId())
                                      .get();
    Preconditions.checkNotNull(planExecution);
    return Ambiance.fromExecutionInstances(planExecution, nodeExecution);
  }
}
