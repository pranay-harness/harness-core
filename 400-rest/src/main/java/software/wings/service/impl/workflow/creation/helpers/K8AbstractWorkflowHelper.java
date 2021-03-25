package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.data.structure.HasPredicate.hasNone;

import software.wings.beans.CanaryOrchestrationWorkflow;

public abstract class K8AbstractWorkflowHelper extends PhaseHelper {
  public boolean isCreationRequired(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    return hasNone(canaryOrchestrationWorkflow.getWorkflowPhases());
  }
}
