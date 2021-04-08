package io.harness.advisers.manualIntervention;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.execution.Status.INTERVENTION_WAITING;

import io.harness.advisers.CommonAdviserTypes;
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
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.Duration;
import java.util.Collections;

@OwnedBy(HarnessTeam.CDC)
public class ManualInterventionAdviserWithRollback implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(CommonAdviserTypes.MANUAL_INTERVENTION_WITH_ROLLBACK.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    ManualInterventionAdviserRollbackParameters parameters = extractParameters(advisingEvent);
    Duration timeout = Duration.newBuilder().setSeconds(java.time.Duration.ofDays(1).toMinutes() * 60).build();
    if (parameters != null && parameters.getTimeout() != null) {
      timeout = Duration.newBuilder().setSeconds(parameters.getTimeout()).build();
    }
    String nextNodeId = parameters == null ? null : parameters.getNextNodeId();
    RepairActionCode repairActionCode = parameters == null ? null : parameters.getTimeoutAction();
    return AdviserResponse.newBuilder()
        .setInterventionWaitAdvise(
            InterventionWaitAdvise.newBuilder()
                .setTimeout(timeout)
                .setRepairActionCode(repairActionCode == null ? RepairActionCode.UNKNOWN : repairActionCode)
                .setNextNodeId(nextNodeId == null ? "" : nextNodeId)
                .build())
        .setType(AdviseType.INTERVENTION_WAIT)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus())
        && advisingEvent.getFromStatus() != INTERVENTION_WAITING;
    ManualInterventionAdviserRollbackParameters parameters = extractParameters(advisingEvent);
    FailureInfo failureInfo = advisingEvent.getNodeExecution().getFailureInfo();
    if (failureInfo != null && parameters != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  private ManualInterventionAdviserRollbackParameters extractParameters(AdvisingEvent advisingEvent) {
    byte[] adviserParameters = advisingEvent.getAdviserParameters();
    if (isEmpty(adviserParameters)) {
      return null;
    }
    return (ManualInterventionAdviserRollbackParameters) kryoSerializer.asObject(adviserParameters);
  }
}
