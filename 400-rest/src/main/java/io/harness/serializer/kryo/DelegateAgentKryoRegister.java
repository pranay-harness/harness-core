package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class DelegateAgentKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to do
  }
}
