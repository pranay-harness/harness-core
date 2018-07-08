package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import software.wings.beans.BlueGreenOrchestrationWorkflow.BlueGreenOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.yaml.workflow.BlueGreenWorkflowYaml;

import java.util.List;

@Singleton
public class BlueGreenWorkflowYamlHandler extends WorkflowYamlHandler<BlueGreenWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    BlueGreenOrchestrationWorkflowBuilder blueGreenOrchestrationWorkflowBuilder =
        BlueGreenOrchestrationWorkflowBuilder.aBlueGreenOrchestrationWorkflow();

    List<WorkflowPhase> phaseList = workflowInfo.getPhaseList();
    if (isNotEmpty(phaseList)) {
      WorkflowPhase workflowPhase = phaseList.get(0);
      workflow.withInfraMappingId(workflowPhase.getInfraMappingId()).withServiceId(workflowPhase.getServiceId());
    }

    blueGreenOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(phaseList);
    workflow.withOrchestrationWorkflow(blueGreenOrchestrationWorkflowBuilder.build());
  }

  @Override
  public BlueGreenWorkflowYaml toYaml(Workflow bean, String appId) {
    BlueGreenWorkflowYaml blueGreenWorkflowYaml = BlueGreenWorkflowYaml.builder().build();
    toYaml(blueGreenWorkflowYaml, bean, appId);
    return blueGreenWorkflowYaml;
  }
}
