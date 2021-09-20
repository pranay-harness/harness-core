package software.wings.service.intfc.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.OrchestrationWorkflowType;

import software.wings.service.impl.workflow.creation.WorkflowCreator;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface WorkflowCreatorFactory {
  WorkflowCreator getWorkflowCreator(OrchestrationWorkflowType type);
}
