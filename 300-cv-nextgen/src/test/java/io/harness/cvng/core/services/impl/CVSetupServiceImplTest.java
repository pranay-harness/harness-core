package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.beans.OnboardingStep.MONITORING_SOURCE;
import static io.harness.cvng.core.beans.OnboardingStep.VERIFICATION_JOBS;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.services.api.KubernetesActivitySourceService;
import io.harness.cvng.core.beans.CVSetupStatusDTO;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

public class CVSetupServiceImplTest extends CategoryTest {
  @InjectMocks private CVSetupServiceImpl cvSetupService;
  @Mock private CVConfigService cvConfigService;
  @Mock private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Mock private VerificationJobService verificationJobService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testgetSetupStatus() {
    String accountId = "accountId";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    when(cvConfigService.doesAnyCVConfigExistsInProject(eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(true);
    when(kubernetesActivitySourceService.doesAActivitySourceExistsForThisProject(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(false);
    when(verificationJobService.doesAVerificationJobExistsForThisProject(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(true);
    CVSetupStatusDTO cvSetupStatus = cvSetupService.getSetupStatus(accountId, orgIdentifier, projectIdentifier);
    assertThat(cvSetupStatus).isNotNull();
    assertThat(cvSetupStatus.getStepsWhichAreCompleted())
        .isEqualTo(Arrays.asList(MONITORING_SOURCE, VERIFICATION_JOBS));
  }
}