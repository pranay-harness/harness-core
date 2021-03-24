package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDC)
public class OrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {}
}
