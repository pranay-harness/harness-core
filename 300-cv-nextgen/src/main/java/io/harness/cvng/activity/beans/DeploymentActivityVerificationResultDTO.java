/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.beans;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class DeploymentActivityVerificationResultDTO {
  String tag;
  String serviceName;
  String serviceIdentifier;
  ActivityVerificationSummary preProductionDeploymentSummary;
  ActivityVerificationSummary productionDeploymentSummary;
  ActivityVerificationSummary postDeploymentSummary;
}
