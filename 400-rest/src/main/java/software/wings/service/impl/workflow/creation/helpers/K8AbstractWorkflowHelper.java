package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.CanaryOrchestrationWorkflow;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public abstract class K8AbstractWorkflowHelper extends PhaseHelper {
  public boolean isCreationRequired(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    return isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases());
  }
}
