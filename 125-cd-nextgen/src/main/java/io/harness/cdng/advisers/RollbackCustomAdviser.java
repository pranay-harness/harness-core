/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.advisers;

import io.harness.advisers.rollback.OnFailRollbackOutput;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PIPELINE)
public class RollbackCustomAdviser implements Adviser {
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(CDAdviserTypes.ROLLBACK_CUSTOM.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    OnFailRollbackOutput rollbackOutcome = getRollbackOutput(advisingEvent);
    if (rollbackOutcome == null) {
      return null;
    }
    String nextNodeId = rollbackOutcome.getNextNodeId();
    NextStepAdvise.Builder builder = NextStepAdvise.newBuilder();
    if (EmptyPredicate.isNotEmpty(nextNodeId)) {
      builder.setNextNodeId(nextNodeId);
    }
    return AdviserResponse.newBuilder().setNextStepAdvise(builder.build()).setType(AdviseType.NEXT_STEP).build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    OnFailRollbackOutput rollbackOutcome = getRollbackOutput(advisingEvent);
    if (rollbackOutcome == null) {
      return false;
    }
    return StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus());
  }

  private OnFailRollbackOutput getRollbackOutput(AdvisingEvent advisingEvent) {
    OptionalSweepingOutput optionalSweepingOutput =
        executionSweepingOutputService.resolveOptional(advisingEvent.getAmbiance(),
            RefObjectUtils.getSweepingOutputRefObject(YAMLFieldNameConstants.USE_ROLLBACK_STRATEGY));
    if (!optionalSweepingOutput.isFound()) {
      return null;
    }
    return (OnFailRollbackOutput) optionalSweepingOutput.getOutput();
  }
}
