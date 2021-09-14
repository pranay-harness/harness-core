/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataCollectionRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnboardingRequestDTO {
  private String connectorIdentifier;
  private String tracingId;
  private String accountId;
  private String projectIdentifier;
  private String orgIdentifier;
  private DataCollectionRequest dataCollectionRequest;
}
