package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.plan.input.DummyOutput;
import io.harness.serializer.KryoRegistrar;

public class OrchestrationBeansTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    int index = 25 * 10000;
    kryo.register(DummyOutput.class, index++);
  }
}