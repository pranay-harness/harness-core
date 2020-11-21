package io.harness.serializer.kryo;

import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.status.BuildChecksUpdateParameter;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.ngpipeline.status.BuildUpdateType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGPipelineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NgPipeline.class, 390001);
    kryo.register(BuildUpdateType.class, 390003);
    kryo.register(BuildStatusUpdateParameter.class, 390004);
    kryo.register(BuildChecksUpdateParameter.class, 390005);
  }
}
