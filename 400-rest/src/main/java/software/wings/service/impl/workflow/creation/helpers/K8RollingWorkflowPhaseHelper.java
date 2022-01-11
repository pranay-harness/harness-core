package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING_ROLLBACK;
import static software.wings.sm.StateType.RANCHER_K8S_DEPLOYMENT_ROLLING;
import static software.wings.sm.StateType.RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK;
import static software.wings.sm.StateType.RANCHER_RESOLVE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.common.WorkflowConstants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class K8RollingWorkflowPhaseHelper extends K8AbstractWorkflowHelper {
  // Get all Rolling Steps
  @Override
  public List<PhaseStep> getWorkflowPhaseSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(getRollingDeployPhaseStep());
    phaseSteps.add(getRollingVerifyPhaseStep());
    phaseSteps.add(getRollingWrapUpPhaseStep());
    return phaseSteps;
  }

  // Get all Rolling Rollback Steps
  @Override
  public List<PhaseStep> getRollbackPhaseSteps() {
    List<PhaseStep> phaseSteps = new ArrayList<>();
    phaseSteps.add(getRollingRollbackDeployPhaseStep());
    phaseSteps.add(getRollingRollbackWrapUpPhaseStep());
    return phaseSteps;
  }

  // Steps for Rolling

  private PhaseStep getRollingDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(K8S_DEPLOYMENT_ROLLING.name())
                     .name(WorkflowConstants.K8S_DEPLOYMENT_ROLLING)
                     .properties(new HashMap<>())
                     .build())
        .build();
  }

  PhaseStep getRollingVerifyPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, "Verify").build();
  }

  PhaseStep getRollingWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP).build();
  }

  // Steps for Rolling Rollback

  private PhaseStep getRollingRollbackDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(K8S_DEPLOYMENT_ROLLING_ROLLBACK.name())
                     .name(WorkflowConstants.K8S_DEPLOYMENT_ROLLING_ROLLBACK)
                     .rollback(true)
                     .build())
        .withPhaseStepNameForRollback(WorkflowServiceHelper.DEPLOY)
        .withStatusForRollback(ExecutionStatus.SUCCESS)
        .withRollback(true)
        .build();
  }

  private PhaseStep getRancherRollingRollbackDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK.name())
                     .name(WorkflowConstants.RANCHER_K8S_DEPLOYMENT_ROLLING_ROLLBACK)
                     .rollback(true)
                     .build())
        .withPhaseStepNameForRollback(WorkflowServiceHelper.DEPLOY)
        .withStatusForRollback(ExecutionStatus.SUCCESS)
        .withRollback(true)
        .build();
  }

  PhaseStep getRollingRollbackWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP)
        .withPhaseStepNameForRollback(WorkflowServiceHelper.WRAP_UP)
        .withRollback(true)
        .build();
  }
}
