package software.wings.service.impl.yaml.handler.workflow;

import software.wings.beans.BuildWorkflow.BuildWorkflowBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowPhase;
import software.wings.utils.Util;
import software.wings.yaml.workflow.BuildWorkflowYaml;

import java.util.List;

/**
 * @author rktummala on 11/1/17
 */
public class BuildWorkflowYamlHandler extends WorkflowYamlHandler<BuildWorkflowYaml> {
  @Override
  protected void setOrchestrationWorkflow(WorkflowInfo workflowInfo, WorkflowBuilder workflow) {
    BuildWorkflowBuilder buildWorkflowBuilder = BuildWorkflowBuilder.aBuildWorkflow();

    List<WorkflowPhase> phaseList = workflowInfo.getPhaseList();
    if (!Util.isEmpty(phaseList)) {
      WorkflowPhase workflowPhase = phaseList.get(0);
      workflow.withInfraMappingId(workflowPhase.getInfraMappingId()).withServiceId(workflowPhase.getServiceId());
    }

    buildWorkflowBuilder.withFailureStrategies(workflowInfo.getFailureStrategies())
        .withNotificationRules(workflowInfo.getNotificationRules())
        .withPostDeploymentSteps(workflowInfo.getPostDeploymentSteps())
        .withPreDeploymentSteps(workflowInfo.getPreDeploymentSteps())
        .withRollbackWorkflowPhaseIdMap(workflowInfo.getRollbackPhaseMap())
        .withUserVariables(workflowInfo.getUserVariables())
        .withWorkflowPhases(phaseList);
    workflow.withOrchestrationWorkflow(buildWorkflowBuilder.build());
  }

  @Override
  public BuildWorkflowYaml toYaml(Workflow bean, String appId) {
    BuildWorkflowYaml buildWorkflowYaml = BuildWorkflowYaml.builder().build();
    toYaml(buildWorkflowYaml, bean, appId);
    return buildWorkflowYaml;
  }
}
