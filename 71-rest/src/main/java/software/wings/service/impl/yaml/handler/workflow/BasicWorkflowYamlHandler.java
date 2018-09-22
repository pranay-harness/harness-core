package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.yaml.workflow.BasicWorkflowYaml;

import java.util.List;
/**
 * @author rktummala on 11/1/17
 */
@Singleton
public class BasicWorkflowYamlHandler extends WorkflowYamlHandler<BasicWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    BasicOrchestrationWorkflowBuilder basicOrchestrationWorkflowBuilder =
        BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow();

    List<WorkflowPhase> phaseList = workflowInfo.getPhaseList();
    if (isNotEmpty(phaseList)) {
      WorkflowPhase workflowPhase = phaseList.get(0);
      workflow.withInfraMappingId(workflowPhase.getInfraMappingId()).withServiceId(workflowPhase.getServiceId());
    }

    basicOrchestrationWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(phaseList);
    workflow.withOrchestrationWorkflow(basicOrchestrationWorkflowBuilder.build());
  }

  @Override
  public BasicWorkflowYaml toYaml(Workflow bean, String appId) {
    BasicWorkflowYaml basicWorkflowYaml = BasicWorkflowYaml.builder().build();
    toYaml(basicWorkflowYaml, bean, appId);
    return basicWorkflowYaml;
  }
}
