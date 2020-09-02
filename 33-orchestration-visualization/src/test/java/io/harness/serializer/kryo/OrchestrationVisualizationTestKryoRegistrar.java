package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.utils.DummyForkStepParameters;
import io.harness.utils.DummyOutcome;
import io.harness.utils.DummySectionStepParameters;

public class OrchestrationVisualizationTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    int index = 33 * 10000;
    kryo.register(DummyForkStepParameters.class, index++);
    kryo.register(DummySectionStepParameters.class, index++);
    kryo.register(DummyOutcome.class, index++);
  }
}
