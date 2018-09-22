package software.wings.service.impl.yaml.handler.workflow;

import com.google.inject.Singleton;

import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.yaml.workflow.MultiServiceWorkflowYaml;

/**
 * @author rktummala on 11/1/17
 */
@Singleton
public class MultiServiceWorkflowYamlHandler extends WorkflowYamlHandler<MultiServiceWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    MultiServiceOrchestrationWorkflowBuilder multiServiceWorkflowBuilder =
        MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow();

    MultiServiceOrchestrationWorkflow orchestrationWorkflow =
        multiServiceWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
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
  public MultiServiceWorkflowYaml toYaml(Workflow bean, String appId) {
    MultiServiceWorkflowYaml multiServiceWorkflowYaml = MultiServiceWorkflowYaml.builder().build();
    toYaml(multiServiceWorkflowYaml, bean, appId);
    return multiServiceWorkflowYaml;
  }
}
