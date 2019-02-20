package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;

public class OrchestrationKryoRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(ContextElementType.class, 4004);
  }
}
