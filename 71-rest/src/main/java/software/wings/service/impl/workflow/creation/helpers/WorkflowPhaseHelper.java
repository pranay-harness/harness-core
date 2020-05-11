package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

@OwnedBy(CDC)
public class WorkflowPhaseHelper {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private ServiceResourceService serviceResourceService;

  public WorkflowPhase createRollbackPhase(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .name(WorkflowServiceHelper.ROLLBACK_PREFIX + workflowPhase.getName())
        .rollback(true)
        .serviceId(workflowPhase.getServiceId())
        .computeProviderId(workflowPhase.getComputeProviderId())
        .infraMappingName(workflowPhase.getInfraMappingName())
        .phaseNameForRollback(workflowPhase.getName())
        .deploymentType(workflowPhase.getDeploymentType())
        .infraMappingId(workflowPhase.getInfraMappingId())
        .infraDefinitionId(workflowPhase.getInfraDefinitionId())
        .build();
  }

  public void setCloudProvider(String accountId, String appId, WorkflowPhase workflowPhase) {
    if (featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, accountId)) {
      workflowServiceHelper.setCloudProviderInfraRefactor(appId, workflowPhase);
    } else {
      workflowServiceHelper.setCloudProvider(appId, workflowPhase);
    }
  }

  public void addK8sEmptyPhaseStep(WorkflowPhase workflowPhase) {
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY).build());
  }

  public void addK8sEmptyRollbackPhaseStep(WorkflowPhase rollbackPhase) {
    List<PhaseStep> rollbackPhaseSteps = rollbackPhase.getPhaseSteps();
    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
                               .withPhaseStepNameForRollback(WorkflowServiceHelper.DEPLOY)
                               .withRollback(true)
                               .build());
  }

  public boolean isDaemonSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, KUBERNETES.name());
    return containerTask != null && containerTask.checkDaemonSet();
  }

  public boolean isStatefulSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, KUBERNETES.name());
    return containerTask != null && containerTask.checkStatefulSet();
  }

  public void setCloudProviderIfNeeded(Workflow workflow, WorkflowPhase workflowPhase) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.needCloudProvider()) {
      setCloudProvider(workflow.getAccountId(), workflow.getAppId(), workflowPhase);
    }
  }

  public boolean addPhaseIfStepsGenerated(Workflow workflow, WorkflowPhase workflowPhase) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    // No need to generate phase steps if it's already created
    if (isNotEmpty(workflowPhase.getPhaseSteps()) && orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases().add(workflowPhase);
      return true;
    } else {
      return false;
    }
  }
}
