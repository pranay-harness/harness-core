package software.wings.service.impl.workflow.creation;

import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import io.harness.serializer.MapperUtils;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;

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
