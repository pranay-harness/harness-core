package io.harness.serializer.morphia.converters;

import io.harness.capability.CapabilityParameters;
import io.harness.persistence.converters.ProtoMessageConverter;

public class CapabilityParametersMorphiaConverter extends ProtoMessageConverter<CapabilityParameters> {
  public CapabilityParametersMorphiaConverter() {
    super(CapabilityParameters.class);
  }
}
