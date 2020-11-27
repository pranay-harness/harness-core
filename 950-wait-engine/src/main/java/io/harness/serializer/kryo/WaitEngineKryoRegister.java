package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;
import io.harness.waiter.StringNotifyProgressData;
import io.harness.waiter.StringNotifyResponseData;

import com.esotericsoftware.kryo.Kryo;

public class WaitEngineKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(StringNotifyResponseData.class, 5271);
    kryo.register(StringNotifyProgressData.class, 5700);
  }
}
