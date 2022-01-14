package io.harness.serializer.kryo;

import io.harness.delegate.beans.Delegate;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.DelegateTaskUsageInsightsEventType;

import com.esotericsoftware.kryo.Kryo;

public class DelegateServiceKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to do
    kryo.register(PerpetualTaskClientContext.class, 40030);
    kryo.register(Delegate.class, 40031);
    kryo.register(DelegateTaskUsageInsightsEventType.class, 40032);
  }
}
