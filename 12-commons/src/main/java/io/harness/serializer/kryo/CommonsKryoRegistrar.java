package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.exception.VerificationOperationException;
import io.harness.serializer.KryoRegistrar;

public class CommonsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(VerificationOperationException.class, 3001);
  }
}
