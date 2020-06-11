package io.harness.yaml.core.deserializer;

import io.harness.yaml.core.intfc.StepInfo;

public class StepPolymorphicDeserializer extends PropertyBindingPolymorphicDeserializer<StepInfo> {
  public StepPolymorphicDeserializer() {
    super(StepInfo.class);
  }
  public StepPolymorphicDeserializer(Class<StepInfo> clazz) {
    super(clazz);
  }
}
