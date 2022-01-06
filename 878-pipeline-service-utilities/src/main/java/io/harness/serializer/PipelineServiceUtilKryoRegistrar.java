/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
