package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.InterruptEffect;
import io.harness.interrupts.RepairActionCode;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import java.time.Duration;

/**
 * We are trying to remain as independent from Kryo as possible.
 * All the classes which get saved inside DelegateResponseData need to be registered as our
 * WaitNotify engine used that.
 */
@OwnedBy(CDC)
public class OrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // Add new Classes Here
    kryo.register(NodeExecution.class, 2506);
    kryo.register(Duration.class, 2516);
    kryo.register(OutcomeInstance.class, 2517);

    kryo.register(RepairActionCode.class, 2528);

    kryo.register(InterruptEffect.class, 2534);

    // Add moved/old classes here
    // Keeping the same id for moved classes
    kryo.register(ExecutionInterruptType.class, 4000);
  }
}
