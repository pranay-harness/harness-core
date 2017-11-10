package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.yaml.workflow.BasicWorkflowYaml;
import software.wings.yaml.workflow.BasicWorkflowYaml.Builder;

/**
 * @author rktummala on 11/1/17
 */
public class BasicWorkflowYamlHandler extends WorkflowYamlHandler<BasicWorkflowYaml, BasicWorkflowYaml.Builder> {
  @Override
  protected OrchestrationWorkflow constructOrchestrationWorkflow(WorkflowInfo workflowInfo) {
    BasicOrchestrationWorkflowBuilder basicOrchestrationWorkflowBuilder =
        BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow();
    basicOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(workflowInfo.getPhaseList());
    return basicOrchestrationWorkflowBuilder.build();
  }

  @Override
  protected Builder getYamlBuilder() {
    return BasicWorkflowYaml.Builder.aYaml();
  }
}
