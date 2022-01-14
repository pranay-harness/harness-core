package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import ci.pipeline.execution.CIAccountExecutionMetadata;
import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.CI)
public class CIExecutionKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CIAccountExecutionMetadata.class, 100101);
  }
}
