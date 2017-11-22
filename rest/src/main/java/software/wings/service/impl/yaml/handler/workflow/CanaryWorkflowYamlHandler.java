package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.yaml.workflow.CanaryWorkflowYaml;

import java.util.List;

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

  @Override
  public Workflow upsertFromYaml(ChangeContext<CanaryWorkflowYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }
}
