package io.harness.serializer.kryo;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class DelegateServiceKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to do
    kryo.register(PerpetualTaskClientContext.class, 40030);
  }
}
