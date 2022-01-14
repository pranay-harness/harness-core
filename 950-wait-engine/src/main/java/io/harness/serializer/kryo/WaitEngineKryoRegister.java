package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;
import io.harness.waiter.StringNotifyProgressData;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitInstanceTimeoutCallback;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.DEL)
public class WaitEngineKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(StringNotifyResponseData.class, 5271);
    kryo.register(StringNotifyProgressData.class, 5700);

    kryo.register(WaitInstanceTimeoutCallback.class, 95002);
  }
}
