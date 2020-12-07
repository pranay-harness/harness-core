package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class OrchestrationBeansTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // Nothing to register
  }
}
