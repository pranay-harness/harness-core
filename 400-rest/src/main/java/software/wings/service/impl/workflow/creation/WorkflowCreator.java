package software.wings.service.impl.workflow.creation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
public abstract class WorkflowCreator {
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;

  public abstract Workflow createWorkflow(Workflow clientWorkflow);

  public abstract void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase);

  void addLinkedPreOrPostDeploymentSteps(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        canaryOrchestrationWorkflow.getPreDeploymentSteps(), null);
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        canaryOrchestrationWorkflow.getPostDeploymentSteps(), null);
  }
}
