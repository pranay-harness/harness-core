package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.yaml.workflow.MultiServiceWorkflowYaml;

import java.util.List;

/**
 * @author rktummala on 11/1/17
 */
public class MultiServiceWorkflowYamlHandler
    extends WorkflowYamlHandler<MultiServiceWorkflowYaml, MultiServiceWorkflowYaml.Builder> {
  @Override
  protected OrchestrationWorkflow constructOrchestrationWorkflow(WorkflowInfo workflowInfo) {
    MultiServiceOrchestrationWorkflowBuilder multiServiceWorkflowBuilder =
        MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow();

    return multiServiceWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(workflowInfo.getPhaseList())
        .build();
  }

  @Override
  protected MultiServiceWorkflowYaml.Builder getYamlBuilder() {
    return MultiServiceWorkflowYaml.Builder.aYaml();
  }

  @Override
  public Workflow upsertFromYaml(ChangeContext<MultiServiceWorkflowYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }
}
