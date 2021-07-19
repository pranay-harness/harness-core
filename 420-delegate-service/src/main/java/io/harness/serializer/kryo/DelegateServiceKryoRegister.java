package io.harness.serializer.kryo;

import io.harness.delegate.beans.DelegateTaskBroadcast;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class DelegateServiceKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to do
    kryo.register(DelegateTaskBroadcast.class, 7428);
  }
}
