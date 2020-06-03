package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.utils.DummyOutcome;
import io.harness.utils.steps.TestStepParameters;

import java.util.Set;

public class OrchestrationTestMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(HelperPut h, HelperPut w) {
    h.put("utils.DummyOutcome", DummyOutcome.class);
    h.put("utils.steps.TestStepParameters", TestStepParameters.class);
  }
}
