/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import com.google.inject.Singleton;

@Singleton

public class AlwaysFalseValidationCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    AlwaysFalseValidationCapability ignoreValidationCapability = (AlwaysFalseValidationCapability) delegateCapability;

    return CapabilityResponse.builder().delegateCapability(ignoreValidationCapability).validated(false).build();
  }
}
