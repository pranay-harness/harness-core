package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.yaml.workflow.BasicWorkflowYaml;
import software.wings.yaml.workflow.BasicWorkflowYaml.Builder;

import java.util.List;

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

  @Override
  public Workflow upsertFromYaml(ChangeContext<BasicWorkflowYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }
}
