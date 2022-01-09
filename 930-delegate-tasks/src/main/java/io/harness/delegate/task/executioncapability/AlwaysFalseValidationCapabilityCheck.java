/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
