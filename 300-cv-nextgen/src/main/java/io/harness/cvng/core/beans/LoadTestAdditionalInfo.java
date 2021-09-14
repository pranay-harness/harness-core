/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.verificationjob.beans.AdditionalInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoadTestAdditionalInfo extends AdditionalInfo {
  String baselineDeploymentTag;
  Long baselineStartTime;
  String currentDeploymentTag;
  long currentStartTime;

  public boolean isBaselineRun() {
    if (baselineDeploymentTag == null) {
      return true;
    }
    return false;
  }
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }
}
