package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.advisers.manualIntervention.ManualInterventionAdviserRollbackParameters;
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
  }
}
