package io.harness.pms.sdk.core.adviser.rollback;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.RollbackStrategy;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@OwnedBy(PL)
public class RollbackCustomAdviser implements Adviser {
  @Inject private OutcomeService outcomeService;

  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ROLLBACK_CUSTOM.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    RollbackOutcome rollbackOutcome = getRollbackOutcome(advisingEvent);
    String nextNodeId = null;
    RollbackInfo rollbackInfo = Objects.requireNonNull(rollbackOutcome).getRollbackInfo();
    if (rollbackInfo.getStrategy().equals(RollbackStrategy.STAGE_ROLLBACK)) {
      if (rollbackInfo.getGroup().equals(RollbackNodeType.STEP_GROUP.name())) {
        nextNodeId = rollbackInfo.getNodeTypeToUuid().get(RollbackNodeType.STEP_GROUP_COMBINED.name());
      } else if (rollbackInfo.getGroup().equals(RollbackNodeType.DIRECT_STAGE.name())) {
        nextNodeId = rollbackInfo.getNodeTypeToUuid().get(RollbackNodeType.BOTH_STEP_GROUP_STAGE.name());
      } else if (rollbackInfo.getGroup().equals(RollbackNodeType.STAGE.name())) {
        nextNodeId = rollbackInfo.getNodeTypeToUuid().get(RollbackNodeType.STAGE.name());
      }
    } else if (rollbackInfo.getStrategy().equals(RollbackStrategy.STEP_GROUP_ROLLBACK)) {
      if (rollbackInfo.getGroup().equals(RollbackNodeType.STEP_GROUP.name())) {
        nextNodeId = rollbackInfo.getNodeTypeToUuid().get(RollbackNodeType.STEP_GROUP.name());
      }
    }

    NextStepAdvise.Builder builder = NextStepAdvise.newBuilder();
    if (hasSome(nextNodeId)) {
      builder.setNextNodeId(nextNodeId);
    }
    return AdviserResponse.newBuilder().setNextStepAdvise(builder.build()).setType(AdviseType.NEXT_STEP).build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    RollbackOutcome rollbackOutcome = getRollbackOutcome(advisingEvent);
    if (rollbackOutcome == null || rollbackOutcome.getRollbackInfo() == null) {
      return false;
    }
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getNodeExecution().getFailureInfo();
    if (failureInfo != null && !hasNone(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(
              rollbackOutcome.getRollbackInfo().getFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  private RollbackOutcome getRollbackOutcome(AdvisingEvent advisingEvent) {
    List<StepOutcomeRef> outcomeRefs = advisingEvent.getNodeExecution().getOutcomeRefsList();
    for (StepOutcomeRef outcomeRef : outcomeRefs) {
      Outcome outcome = outcomeService.fetchOutcome(outcomeRef.getInstanceId());
      if (outcome instanceof RollbackOutcome) {
        return (RollbackOutcome) outcome;
      }
    }
    return null;
  }
}
