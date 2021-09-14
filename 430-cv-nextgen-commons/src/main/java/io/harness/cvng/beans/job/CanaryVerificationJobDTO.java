/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.job;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("CANARY")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class CanaryVerificationJobDTO extends VerificationJobDTO {
  private String sensitivity;
  private String trafficSplitPercentage;

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }
}
