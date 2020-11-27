package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;
import io.harness.utils.DummySweepingOutput;

import com.esotericsoftware.kryo.Kryo;

public class OrchestrationTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    int index = 31 * 10000;
    kryo.register(DummySweepingOutput.class, index++);
  }
}
