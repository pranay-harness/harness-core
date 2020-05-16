package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.utils.DummySweepingOutput;

public class OrchestrationTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    int index = 31 * 10000;
    kryo.register(DummySweepingOutput.class, index++);
  }
}
