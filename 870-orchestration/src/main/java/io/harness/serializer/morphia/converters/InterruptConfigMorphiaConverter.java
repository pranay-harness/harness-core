package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.advisers.InterruptConfig;

public class InterruptConfigMorphiaConverter extends ProtoMessageConverter<InterruptConfig> {
  public InterruptConfigMorphiaConverter() {
    super(InterruptConfig.class);
  }
}
