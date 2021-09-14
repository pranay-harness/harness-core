/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingResponseDTO {
  private String accountId;
  private Object result;
  private String projectIdentifier;
  private String orgIdentifier;
  private String connectorIdentifier;
  private String tracingId;
}
