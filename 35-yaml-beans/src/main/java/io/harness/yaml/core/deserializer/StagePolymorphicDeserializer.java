package io.harness.yaml.core.deserializer;

import io.harness.yaml.core.intfc.Stage;

/**
 *  jackson fails in case wrapper types are interfaces. this deserializer ensures interfaces correct implementation are
 * selected even in context of wrapper object
 */
public class StagePolymorphicDeserializer extends PropertyBindingPolymorphicDeserializer<Stage> {
  public StagePolymorphicDeserializer() {
    super(Stage.class);
  }
  public StagePolymorphicDeserializer(Class<Stage> clazz) {
    super(clazz);
  }
}
