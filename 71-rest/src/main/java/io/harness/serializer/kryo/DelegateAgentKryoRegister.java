package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

public class DelegateAgentKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelegateConnectionResult.class, 6000);
  }
}
