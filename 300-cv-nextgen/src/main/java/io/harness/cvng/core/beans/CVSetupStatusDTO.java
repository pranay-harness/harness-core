/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@ApiModel("CVSetupStatus")
public class CVSetupStatusDTO {
  List<OnboardingStep> stepsWhichAreCompleted;
  int totalNumberOfServices;
  int totalNumberOfEnvironments;
  int numberOfServicesUsedInMonitoringSources;
  int numberOfServicesUsedInActivitySources;
  int servicesUndergoingHealthVerification;
}
