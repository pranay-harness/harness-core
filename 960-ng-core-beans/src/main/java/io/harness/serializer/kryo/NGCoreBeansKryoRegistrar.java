package io.harness.serializer.kryo;

import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGCoreBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EnvironmentType.class, 20100);
  }
}
