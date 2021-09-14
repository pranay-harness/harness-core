/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.spring.converters.stepoutcomeref;

import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.serializer.spring.ProtoReadConverter;

public class StepOutcomeRefReadConverter extends ProtoReadConverter<StepOutcomeRef> {
  public StepOutcomeRefReadConverter() {
    super(StepOutcomeRef.class);
  }
}
