package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(DX)
public class ConnectorNextGenKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to do
  }
}
