package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import software.wings.api.DeploymentEvent;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.KmsTransitionEvent;
import software.wings.collect.CollectEvent;
import software.wings.service.impl.DelayEvent;
import software.wings.service.impl.ExecutionEvent;

public class ManagerKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    int index = 71 * 1000;
    kryo.register(InstanceChangeEvent.class, index++);
    kryo.register(CollectEvent.class, index++);
    kryo.register(DelayEvent.class, index++);
    kryo.register(DeploymentEvent.class, index++);
    kryo.register(ExecutionEvent.class, index++);
    kryo.register(KmsTransitionEvent.class, index++);
  }
}
