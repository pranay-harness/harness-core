package io.harness.pms.sdk.core.adviser.manualintervention;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.InterventionWaitAdvise;
import io.harness.pms.contracts.commons.RepairActionCode;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.Duration;
import java.util.Collections;

@OwnedBy(HarnessTeam.PIPELINE)
public class ManualInterventionAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.MANUAL_INTERVENTION.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    ManualInterventionAdviserParameters parameters = extractParameters(advisingEvent);
    Duration timeout = Duration.newBuilder().setSeconds(java.time.Duration.ofDays(1).toMinutes() * 60).build();
    if (parameters != null && parameters.getTimeout() != null) {
      timeout = Duration.newBuilder().setSeconds(parameters.getTimeout()).build();
    }
    RepairActionCode repairActionCode = parameters == null ? null : parameters.getTimeoutAction();
    return AdviserResponse.newBuilder()
        .setInterventionWaitAdvise(
            InterventionWaitAdvise.newBuilder()
                .setTimeout(timeout)
                .setRepairActionCode(repairActionCode == null ? RepairActionCode.UNKNOWN : repairActionCode)
                .setFromStatus(advisingEvent.getToStatus())
                .build())
        .setType(AdviseType.INTERVENTION_WAIT)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus())
        && advisingEvent.getFromStatus() != INTERVENTION_WAITING;
    ManualInterventionAdviserParameters parameters = extractParameters(advisingEvent);
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && parameters != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  private ManualInterventionAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    byte[] adviserParameters = advisingEvent.getAdviserParameters();
    if (isEmpty(adviserParameters)) {
      return null;
    }
    return (ManualInterventionAdviserParameters) kryoSerializer.asObject(adviserParameters);
  }
}
