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
