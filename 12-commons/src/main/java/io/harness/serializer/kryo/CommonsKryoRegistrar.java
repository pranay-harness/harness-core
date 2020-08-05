package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ServiceNowException;
import io.harness.exception.VerificationOperationException;
import io.harness.serializer.KryoRegistrar;

public class CommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(VerificationOperationException.class, 3001);
    kryo.register(ServiceNowException.class, 3002);
    kryo.register(SecretRefData.class, 3003);
  }
}
