package io.harness.serializer.kryo;

import io.harness.accesscontrol.clients.HAccessCheckRequestDTO;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class DecisionModuleRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(HAccessCheckRequestDTO.class, 112350);
  }
}
