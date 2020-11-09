package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.core.beans.CVSetupStatusDTO;
import io.harness.cvng.core.beans.OnboardingStep;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVSetupService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CVSetupServiceImpl implements CVSetupService {
  private CVConfigService cvConfigService;
  private KubernetesActivitySourceService kubernetesActivitySourceService;
  private VerificationJobService verificationJobService;

  @Override
  public CVSetupStatusDTO getSetupStatus(String accountId, String orgIdentifier, String projectIdentifier) {
    List<OnboardingStep> allStepsWhichAreDone = new ArrayList<>();
    boolean doesACVConfigExistsForThisProject =
        cvConfigService.doesAnyCVConfigExistsInProject(accountId, orgIdentifier, projectIdentifier);
    boolean doesAActivitySourceExistsForThisProject =
        kubernetesActivitySourceService.doesAActivitySourceExistsForThisProject(
            accountId, orgIdentifier, projectIdentifier);
    boolean doesAVerificationJobExistsForThisProject =
        verificationJobService.doesAVerificationJobExistsForThisProject(accountId, orgIdentifier, projectIdentifier);
    if (doesAActivitySourceExistsForThisProject) {
      allStepsWhichAreDone.add(OnboardingStep.ACTIVITY_SOURCE);
    }
    if (doesACVConfigExistsForThisProject) {
      allStepsWhichAreDone.add(OnboardingStep.MONITORING_SOURCE);
    }

    if (doesAVerificationJobExistsForThisProject) {
      allStepsWhichAreDone.add(OnboardingStep.VERIFICATION_JOBS);
    }
    return CVSetupStatusDTO.builder().stepsWhichAreCompleted(allStepsWhichAreDone).build();
  }
}
