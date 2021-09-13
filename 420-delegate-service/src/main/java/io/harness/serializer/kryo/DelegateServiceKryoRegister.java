package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import software.wings.beans.PerpetualTaskBroadcastEvent;

import com.esotericsoftware.kryo.Kryo;

public class DelegateServiceKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(PerpetualTaskBroadcastEvent.class, 40015);
  }
}
