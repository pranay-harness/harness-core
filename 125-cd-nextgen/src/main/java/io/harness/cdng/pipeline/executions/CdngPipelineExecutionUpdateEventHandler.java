package io.harness.cdng.pipeline.executions;

import static io.harness.executions.steps.StepSpecTypeConstants.K8S_ROLLING_ROLLBACK;
import static io.harness.executions.steps.StepSpecTypeConstants.TERRAFORM_ROLLBACK;
import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.EXPIRED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.logstreaming.ILogStreamingStepClient;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class CdngPipelineExecutionUpdateEventHandler implements OrchestrationEventHandler {
  private static final Set<String> k8sSteps =
      Sets.newHashSet(StepSpecTypeConstants.K8S_ROLLING_DEPLOY, K8S_ROLLING_ROLLBACK,
          StepSpecTypeConstants.K8S_BLUE_GREEN_DEPLOY, StepSpecTypeConstants.K8S_APPLY, StepSpecTypeConstants.K8S_SCALE,
          StepSpecTypeConstants.K8S_BG_SWAP_SERVICES, StepSpecTypeConstants.K8S_CANARY_DELETE,
          StepSpecTypeConstants.K8S_CANARY_DEPLOY, StepSpecTypeConstants.K8S_DELETE);

  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private StepHelper stepHelper;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    try {
      if (isExpiredOrAborted(event.getStatus()) && isK8sOrTerraformRollback(event.getAmbiance())) {
        log.info(String.format("Rollback %s", EXPIRED.equals(event.getStatus()) ? "has expired." : "was aborted"));
        stepHelper.sendRollbackTelemetryEvent(event.getAmbiance(), event.getStatus());
      }

      if (updateK8sLogStreams(event)) {
        List<String> logKeys = StepUtils.generateLogKeys(event.getAmbiance(), Collections.emptyList());
        if (EmptyPredicate.isNotEmpty(logKeys)) {
          String prefix = logKeys.get(0);
          ILogStreamingStepClient logStreamingStepClient =
              logStreamingStepClientFactory.getLogStreamingStepClient(event.getAmbiance());
          logStreamingStepClient.closeAllOpenStreamsWithPrefix(prefix);
        }
      }
    } catch (Exception ex) {
      log.error("Unable to close log streams", ex);
    }
  }

  private boolean isK8sOrTerraformRollback(Ambiance ambiance) {
    return isStepType(ambiance, K8S_ROLLING_ROLLBACK) || isStepType(ambiance, TERRAFORM_ROLLBACK);
  }

  private boolean isExpiredOrAborted(Status status) {
    return EXPIRED.equals(status) || ABORTED.equals(status);
  }

  private boolean isStepType(Ambiance ambiance, String stepSpecType) {
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    return level != null && level.getStepType() != null && stepSpecType.equals(level.getStepType().getType());
  }

  private boolean updateK8sLogStreams(OrchestrationEvent event) {
    return StatusUtils.isFinalStatus(event.getStatus())
        && k8sSteps.contains(
            Objects.requireNonNull(AmbianceUtils.obtainCurrentLevel(event.getAmbiance())).getStepType().getType());
  }
}
