package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class PmsSdkCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(SweepingOutput.class, 3101);
  }
}
