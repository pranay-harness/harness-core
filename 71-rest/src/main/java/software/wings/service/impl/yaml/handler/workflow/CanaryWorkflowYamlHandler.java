package software.wings.service.impl.yaml.handler.workflow;

import com.google.inject.Singleton;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.yaml.workflow.CanaryWorkflowYaml;

/**
 * @author rktummala on 11/1/17
 */
@Singleton
public class CanaryWorkflowYamlHandler extends WorkflowYamlHandler<CanaryWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    CanaryOrchestrationWorkflowBuilder canaryOrchestrationWorkflowBuilder =
        CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow();

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        canaryOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
            .withNotificationRules(workflowInfo.getNotificationRules())
            .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
            .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
            .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
            .withUserVariables(workflowInfo.getUserVariables())
            .withWorkflowPhases(workflowInfo.getPhaseList())
            .build();
    workflow.withOrchestrationWorkflow(orchestrationWorkflow);
  }

  @Override
  public CanaryWorkflowYaml toYaml(Workflow bean, String appId) {
    CanaryWorkflowYaml canaryWorkflowYaml = CanaryWorkflowYaml.builder().build();
    toYaml(canaryWorkflowYaml, bean, appId);
    return canaryWorkflowYaml;
  }
}
