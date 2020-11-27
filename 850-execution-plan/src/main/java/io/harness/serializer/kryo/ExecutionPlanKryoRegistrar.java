package io.harness.serializer.kryo;

import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class ExecutionPlanKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(KeyAwareStepDependencySpec.class, 36001);
  }
}
