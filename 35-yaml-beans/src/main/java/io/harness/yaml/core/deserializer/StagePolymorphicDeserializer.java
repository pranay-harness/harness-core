package io.harness.yaml.core.deserializer;

import io.harness.yaml.core.intfc.Stage;
import io.harness.yaml.core.intfc.StepInfo;

/**
 *  jackson fails in case wrapper types are interfaces. this deserializer ensures interfaces correct implementation are
 * selected even in context of wrapper object
 */
public class StagePolymorphicDeserializer extends TypeAwarePolymorphicDeserializer<StepInfo> {
  @Override
  public Class<?> getType() {
    return Stage.class;
  }

  @Override
  public String getTypePropertyName() {
    return "type";
  }
}
