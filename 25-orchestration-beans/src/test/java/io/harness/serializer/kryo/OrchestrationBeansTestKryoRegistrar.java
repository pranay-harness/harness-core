package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.utils.levels.PhaseTestLevel;
import io.harness.utils.levels.SectionTestLevel;

public class OrchestrationBeansTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    int index = 25 * 10000;
    kryo.register(PhaseTestLevel.class, index++);
    kryo.register(SectionTestLevel.class, index++);
  }
}