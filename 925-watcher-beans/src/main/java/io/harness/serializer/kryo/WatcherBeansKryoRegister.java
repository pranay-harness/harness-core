package io.harness.serializer.kryo;

import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class WatcherBeansKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelegateScripts.class, 5002);
    kryo.register(DelegateConfiguration.class, 5469);
    kryo.register(DelegateConfiguration.Action.class, 15000);
  }
}
