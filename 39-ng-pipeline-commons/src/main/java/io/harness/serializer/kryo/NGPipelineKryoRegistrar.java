package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.serializer.KryoRegistrar;

public class NGPipelineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CDPipeline.class, 390001);
  }
}
