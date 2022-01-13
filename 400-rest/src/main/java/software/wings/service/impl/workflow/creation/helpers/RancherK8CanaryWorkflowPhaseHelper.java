package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.k8s.model.K8sExpressions.canaryWorkloadExpression;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.sm.StateType.*;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.common.WorkflowConstants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;

@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class RancherK8CanaryWorkflowPhaseHelper extends K8CanaryWorkflowPhaseHelper {
  @Override
  protected PhaseStep getCanaryDeployPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.DEPLOY)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(RANCHER_RESOLVE.name())
                     .name(WorkflowConstants.RANCHER_RESOLVE_CLUSTERS)
                     .properties(new HashMap<>())
                     .build())
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(RANCHER_K8S_CANARY_DEPLOY.name())
                     .name(WorkflowConstants.RANCHER_K8S_CANARY_DEPLOY)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("instances", "1")
                                     .put("instanceUnitType", "COUNT")
                                     .build())
                     .build())
        .build();
  }

  @Override
  protected PhaseStep getCanaryWrapUpPhaseStep() {
    return aPhaseStep(K8S_PHASE_STEP, WorkflowServiceHelper.WRAP_UP)
        .addStep(GraphNode.builder()
                     .id(generateUuid())
                     .type(RANCHER_K8S_DELETE.name())
                     .name(WorkflowConstants.RANCHER_K8S_DELETE)
                     .properties(ImmutableMap.<String, Object>builder()
                                     .put("resources", canaryWorkloadExpression)
                                     .put("instanceUnitType", "COUNT")
                                     .build())
                     .build())
        .build();
  }
}