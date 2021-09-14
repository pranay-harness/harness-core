/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia.converters;

import io.harness.capability.CapabilityParameters;
import io.harness.persistence.converters.ProtoMessageConverter;

public class CapabilityParametersMorphiaConverter extends ProtoMessageConverter<CapabilityParameters> {
  public CapabilityParametersMorphiaConverter() {
    super(CapabilityParameters.class);
  }
}
