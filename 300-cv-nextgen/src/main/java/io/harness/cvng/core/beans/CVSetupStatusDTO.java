package io.harness.cvng.core.beans;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@ApiModel("CVSetupStatus")
public class CVSetupStatusDTO {
  List<OnboardingStep> stepsWhichAreCompleted;
}
