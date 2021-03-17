package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.DelayEventNotifyData;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

/**
 * We are trying to remain as independent from Kryo as possible.
 * All the classes which get saved inside DelegateResponseData need to be registered as our
 * WaitNotify engine used that.
 */
@OwnedBy(CDC)
public class OrchestrationDelayKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelayEventNotifyData.class, 7273);
  }
}
