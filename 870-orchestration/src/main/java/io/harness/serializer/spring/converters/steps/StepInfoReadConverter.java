/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.steps;

import io.harness.pms.contracts.steps.StepInfo;
import io.harness.serializer.spring.ProtoReadConverter;

public class StepInfoReadConverter extends ProtoReadConverter<StepInfo> {
  public StepInfoReadConverter() {
    super(StepInfo.class);
  }
}
