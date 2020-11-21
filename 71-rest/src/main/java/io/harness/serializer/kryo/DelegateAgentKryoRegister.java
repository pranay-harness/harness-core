package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import software.wings.delegatetasks.validation.DelegateConnectionResult;

import com.esotericsoftware.kryo.Kryo;

public class DelegateAgentKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelegateConnectionResult.class, 6000);
  }
}
