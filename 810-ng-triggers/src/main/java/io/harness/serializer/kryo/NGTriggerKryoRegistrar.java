package io.harness.serializer.kryo;

import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGTriggerKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGTriggerConfig.class, 400001);
  }
}
