package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.yaml.workflow.CanaryWorkflowYaml;

/**
 * @author rktummala on 11/1/17
 */
public class CanaryWorkflowYamlHandler extends WorkflowYamlHandler<CanaryWorkflowYaml, CanaryWorkflowYaml.Builder> {
  @Override
  protected OrchestrationWorkflow constructOrchestrationWorkflow(WorkflowInfo workflowInfo) {
    CanaryOrchestrationWorkflowBuilder canaryOrchestrationWorkflowBuilder =
        CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow();

    return canaryOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(workflowInfo.getPhaseList())
        .build();
  }

  @Override
  protected CanaryWorkflowYaml.Builder getYamlBuilder() {
    return CanaryWorkflowYaml.Builder.aYaml();
  }
}
