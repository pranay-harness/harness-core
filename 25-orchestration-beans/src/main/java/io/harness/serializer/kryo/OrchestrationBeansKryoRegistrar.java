package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.status.Status;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.serializer.KryoRegistrar;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StatusNotifyResponseData;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponseNotifyData;

/**
 * We are trying to remain as independent from Kryo as possible.
 * All the classes which get saved inside ResponseData need to be registered as our
 * WaitNotify engine used that.
 */
@OwnedBy(CDC)
public class OrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    // Add new Classes Here
    kryo.register(Status.class, 2501);
    kryo.register(StatusNotifyResponseData.class, 2502);
    kryo.register(StepResponseNotifyData.class, 2519);
    kryo.register(FailureInfo.class, 2520);
    kryo.register(StepOutcome.class, 2521);

    // Add moved/old classes here
    // Keeping the same id for moved classes
    kryo.register(ExecutionInterruptType.class, 4000);
  }
}