/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.executioncapability;

import java.time.Duration;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SmbConnectionCapability implements ExecutionCapability {
  @NonNull String smbUrl;
  @Builder.Default CapabilityType capabilityType = CapabilityType.SMB;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    return smbUrl;
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
