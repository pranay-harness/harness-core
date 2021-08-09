package io.harness.rule;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;
import io.harness.waiter.NotifyEventListenerTest;
import io.harness.waiter.TestNotifyCallback;
import io.harness.waiter.TestProgressCallback;
import io.harness.waiter.TestResponseData;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PIPELINE)
public class WaitEngineTestRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    int index = 26 * 10000;
    kryo.register(TestNotifyCallback.class, index++);
    kryo.register(TestProgressCallback.class, index++);
    kryo.register(TestResponseData.class, index++);
    kryo.register(NotifyEventListenerTest.TestNotifyCallback.class, index++);
    kryo.register(NotifyEventListenerTest.TestProgressCallback.class, index++);
  }
}
