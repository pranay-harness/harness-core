package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.waiter.NotifyEvent;

public class OrchestrationRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(NotifyEvent.class, 1000000);
  }
}
