package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierOutcome;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;

@OwnedBy(CDC)
public class OrchestrationStepsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(BarrierExecutionInstance.class, 3201);
    kryo.register(BarrierResponseData.class, 3202);
    kryo.register(BarrierOutcome.class, 3203);
    kryo.register(ResourceRestraintInstance.class, 3204);
  }
}