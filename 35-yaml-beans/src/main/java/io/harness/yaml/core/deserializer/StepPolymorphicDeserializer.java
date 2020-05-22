package io.harness.yaml.core.deserializer;

import io.harness.yaml.core.intfc.StepInfo;

/**
 * StepPolymorphicDeserializer class is used as deserializer for Step interface
 * Steps in yaml are kept in list whose elements can be steps or parallel or graph sections.
 * At the same rate Step can have type in order to support this dynamical property we need
 * StepPolymorphicDeserializer to determine type.
 */
public class StepPolymorphicDeserializer extends TypeAwarePolymorphicDeserializer<StepInfo> {
  @Override
  public Class<?> getType() {
    return StepInfo.class;
  }

  @Override
  public String getTypePropertyName() {
    return "type";
  }
}
