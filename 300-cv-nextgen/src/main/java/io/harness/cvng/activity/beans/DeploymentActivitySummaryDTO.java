/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.beans;

import io.harness.cvng.activity.beans.DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeploymentActivitySummaryDTO {
  String serviceName;
  String serviceIdentifier;
  String envName;
  String envIdentifier;
  String deploymentTag;
  DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary;
}
