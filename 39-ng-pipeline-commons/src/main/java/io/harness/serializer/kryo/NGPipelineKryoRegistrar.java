package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.serializer.KryoRegistrar;

public class NGPipelineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NgPipeline.class, 390001);
  }
}
