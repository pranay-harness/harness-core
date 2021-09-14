/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.CVSetupStatusDTO;
import io.harness.cvng.core.beans.OnboardingStep;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVSetupService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CVSetupServiceImpl implements CVSetupService {
  private CVConfigService cvConfigService;
  private KubernetesActivitySourceService kubernetesActivitySourceService;
  private VerificationJobService verificationJobService;
  private NextGenService nextGenService;

  @Override
  public CVSetupStatusDTO getSetupStatus(String accountId, String orgIdentifier, String projectIdentifier) {
    boolean doesAVerificationJobExistsForThisProject =
        verificationJobService.doesAVerificationJobExistsForThisProject(accountId, orgIdentifier, projectIdentifier);
    int totalNumberOfServices = nextGenService.getServicesCount(accountId, orgIdentifier, projectIdentifier);
    int totalNumberOfEnvironments = nextGenService.getEnvironmentCount(accountId, orgIdentifier, projectIdentifier);
    int numberOfServicesUsedInActivitySources =
        kubernetesActivitySourceService.getNumberOfKubernetesServicesSetup(accountId, orgIdentifier, projectIdentifier);
    int numberOfServicesUsedInMonitoringSources =
        cvConfigService.getNumberOfServicesSetup(accountId, orgIdentifier, projectIdentifier);
    int servicesUndergoingHealthVerification = verificationJobService.getNumberOfServicesUndergoingHealthVerification(
        accountId, orgIdentifier, projectIdentifier);
    List<OnboardingStep> onBoardingSteps =
        getOnboardingStepsWhichAreCompleted(numberOfServicesUsedInActivitySources > 0,
            numberOfServicesUsedInMonitoringSources > 0, doesAVerificationJobExistsForThisProject);
    return CVSetupStatusDTO.builder()
        .stepsWhichAreCompleted(onBoardingSteps)
        .totalNumberOfEnvironments(totalNumberOfEnvironments)
        .totalNumberOfServices(totalNumberOfServices)
        .numberOfServicesUsedInActivitySources(numberOfServicesUsedInActivitySources)
        .numberOfServicesUsedInMonitoringSources(numberOfServicesUsedInMonitoringSources)
        .servicesUndergoingHealthVerification(servicesUndergoingHealthVerification)
        .build();
  }

  private List<OnboardingStep> getOnboardingStepsWhichAreCompleted(boolean doesAActivitySourceExistsForThisProject,
      boolean doesACVConfigExistsForThisProject, boolean doesAVerificationJobExistsForThisProject) {
    List<OnboardingStep> allStepsWhichAreDone = new ArrayList<>();
    if (doesAActivitySourceExistsForThisProject) {
      allStepsWhichAreDone.add(OnboardingStep.ACTIVITY_SOURCE);
    }
    if (doesACVConfigExistsForThisProject) {
      allStepsWhichAreDone.add(OnboardingStep.MONITORING_SOURCE);
    }

    if (doesAVerificationJobExistsForThisProject) {
      allStepsWhichAreDone.add(OnboardingStep.VERIFICATION_JOBS);
    }
    return allStepsWhichAreDone;
  }

  @Override
  public List<DataSourceType> getSupportedProviders(String accountId, String orgIdentifier, String projectIdentifier) {
    return Arrays.asList(DataSourceType.values());
  }
}
