package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.queue.TestQueuableObject;
import io.harness.serializer.KryoRegistrar;

public class PersistentTestRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(TestQueuableObject.class, 1000000);
  }
}
