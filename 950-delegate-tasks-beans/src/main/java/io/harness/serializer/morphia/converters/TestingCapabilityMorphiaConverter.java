package io.harness.serializer.morphia.converters;

import io.harness.capability.TestingCapability;
import io.harness.persistence.converters.ProtoMessageConverter;

public class TestingCapabilityMorphiaConverter extends ProtoMessageConverter<TestingCapability> {
  public TestingCapabilityMorphiaConverter() {
    super(TestingCapability.class);
  }
}
