/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserWithRollback;
import io.harness.advisers.nextstep.NextStepAdviser;
import io.harness.advisers.retry.RetryAdviserWithRollback;
import io.harness.advisers.rollback.OnFailRollbackAdviser;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class PipelineServiceUtilAdviserRegistrar {
  public Map<AdviserType, Class<? extends Adviser>> getEngineAdvisers() {
    Map<AdviserType, Class<? extends Adviser>> advisersMap = new HashMap<>();
    advisersMap.put(RetryAdviserWithRollback.ADVISER_TYPE, RetryAdviserWithRollback.class);
    advisersMap.put(OnFailRollbackAdviser.ADVISER_TYPE, OnFailRollbackAdviser.class);
    advisersMap.put(ManualInterventionAdviserWithRollback.ADVISER_TYPE, ManualInterventionAdviserWithRollback.class);
    advisersMap.put(NextStepAdviser.ADVISER_TYPE, NextStepAdviser.class);
    return advisersMap;
  }
}
