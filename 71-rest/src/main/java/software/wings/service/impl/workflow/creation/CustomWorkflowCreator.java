package software.wings.service.impl.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.MapperUtils;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;

@OwnedBy(CDC)
public class CustomWorkflowCreator extends WorkflowCreator {
  @Override
  public Workflow createWorkflow(Workflow clientWorkflow) {
    Workflow workflow = aWorkflow().build();
    MapperUtils.mapObject(clientWorkflow, workflow);
    return workflow;
  }

  @Override
  public void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    // Not supported yet
  }
}
