/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia.converters;

import io.harness.capability.TestingCapability;
import io.harness.persistence.converters.ProtoMessageConverter;

public class TestingCapabilityMorphiaConverter extends ProtoMessageConverter<TestingCapability> {
  public TestingCapabilityMorphiaConverter() {
    super(TestingCapability.class);
  }
}
