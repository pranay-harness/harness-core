package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.yaml.workflow.MultiServiceWorkflowYaml;

/**
 * @author rktummala on 11/1/17
 */
@OwnedBy(CDC)
@Singleton
public class MultiServiceWorkflowYamlHandler extends WorkflowYamlHandler<MultiServiceWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    MultiServiceOrchestrationWorkflowBuilder multiServiceWorkflowBuilder =
        MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow();

    MultiServiceOrchestrationWorkflowBuilder orchestrationWorkflowBuilder =
        multiServiceWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
            .withNotificationRules(workflowInfo.getNotificationRules())
            .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
            .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
            .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
            .withUserVariables(workflowInfo.getUserVariables())
            .withWorkflowPhases(workflowInfo.getPhaseList());
    if (workflowInfo.getConcurrencyStrategy() != null) {
      orchestrationWorkflowBuilder.withConcurrencyStrategy(
          ConcurrencyStrategy.buildFromUnit(workflowInfo.getConcurrencyStrategy()));
    }
    workflow.orchestrationWorkflow(orchestrationWorkflowBuilder.build());
  }

  @Override
  public MultiServiceWorkflowYaml toYaml(Workflow bean, String appId) {
    MultiServiceWorkflowYaml multiServiceWorkflowYaml = MultiServiceWorkflowYaml.builder().build();
    toYaml(multiServiceWorkflowYaml, bean, appId);
    return multiServiceWorkflowYaml;
  }
}
