package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stepDetail.StepDetailInstance;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.data.stepparameters.PmsStepParameters;
import io.harness.serializer.KryoRegistrar;
import io.harness.timeout.trackers.active.ActiveTimeoutParameters;

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

    kryo.register(InterruptEffect.class, 2534);

    kryo.register(ActiveTimeoutParameters.class, 2537);

    // Add new classes here
    kryo.register(Interrupt.class, 87601);
    kryo.register(State.class, 87602);
    kryo.register(StepDetailInstance.class, 87603);

    kryo.register(PmsStepParameters.class, 88405);
  }
}
