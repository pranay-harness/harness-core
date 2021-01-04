package software.wings.service.intfc.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;

import software.wings.service.impl.workflow.creation.WorkflowCreator;

@OwnedBy(CDC)
public interface WorkflowCreatorFactory {
  WorkflowCreator getWorkflowCreator(OrchestrationWorkflowType type);
}
