package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pipeline.plan.scratch.lib.io.MapStepParameters;
import io.harness.serializer.KryoRegistrar;

@OwnedBy(CDC)
public class PipelineServiceKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(MapStepParameters.class, 47001);
  }
}
