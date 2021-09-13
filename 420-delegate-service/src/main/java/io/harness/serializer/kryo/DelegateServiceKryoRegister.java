package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;
import software.wings.beans.PerpetualTaskBroadcastEvent;

public class DelegateServiceKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(PerpetualTaskBroadcastEvent.class, 40015);
  }
}
