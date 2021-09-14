/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserRollbackParameters;
import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.advisers.retry.RetryAdviserRollbackParameters;
import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.advisers.rollback.RollbackStrategy;
import io.harness.annotations.dev.OwnedBy;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class PipelineServiceUtilKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(RetryAdviserRollbackParameters.class, 87801);
    kryo.register(RollbackStrategy.class, 87802);
    kryo.register(OnFailRollbackParameters.class, 87803);
    kryo.register(ManualInterventionAdviserRollbackParameters.class, 87804);
    kryo.register(NextStepAdviserParameters.class, 87805);
  }
}
