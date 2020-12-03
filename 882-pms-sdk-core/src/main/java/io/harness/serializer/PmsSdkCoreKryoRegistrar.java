package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class PmsSdkCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(StepOutcome.class, 2521);
    kryo.register(SweepingOutput.class, 3101);
    kryo.register(PassThroughData.class, 2535);

    // New classes here
    kryo.register(PlanNode.class, 88201);
  }
}
