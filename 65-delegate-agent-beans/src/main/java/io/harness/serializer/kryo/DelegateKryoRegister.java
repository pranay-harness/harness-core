package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.serializer.KryoRegistrar;

public class DelegateKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DelegateScripts.class, 5002);
    kryo.register(DelegateConfiguration.class, 5469);

    kryo.register(DelegateRegisterResponse.class, 6501);
    kryo.register(DelegateParams.class, 6502);
    kryo.register(DelegateConnectionHeartbeat.class, 6503);
    kryo.register(DelegateProfileParams.class, 6504);
    kryo.register(DelegateTaskEvent.class, 6505);
    kryo.register(DelegateTaskAbortEvent.class, 6506);
  }
}
