package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.CDC)
public class NGTemplateKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // Nothing to register
  }
}
