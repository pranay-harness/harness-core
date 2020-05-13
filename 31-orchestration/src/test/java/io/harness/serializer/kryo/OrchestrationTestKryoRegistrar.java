package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.utils.DummySweepingOutput;
import io.harness.utils.PhaseTestLevel;
import io.harness.utils.SectionTestLevel;
import io.harness.utils.StepTestLevel;

public class OrchestrationTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    int index = 31 * 10000;
    kryo.register(PhaseTestLevel.class, index++);
    kryo.register(SectionTestLevel.class, index++);
    kryo.register(StepTestLevel.class, index++);
    kryo.register(DummySweepingOutput.class, index++);
  }
}
