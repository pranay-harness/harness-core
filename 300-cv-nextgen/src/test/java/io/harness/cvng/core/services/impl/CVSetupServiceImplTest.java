/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.beans.OnboardingStep.ACTIVITY_SOURCE;
import static io.harness.cvng.core.beans.OnboardingStep.MONITORING_SOURCE;
import static io.harness.cvng.core.beans.OnboardingStep.VERIFICATION_JOBS;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.CVSetupStatusDTO;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVSetupService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class CVSetupServiceImplTest extends CvNextGenTestBase {
  @Inject private CVSetupService cvSetupService;
  @Mock private CVConfigService cvConfigService;
  @Mock private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Mock private VerificationJobService verificationJobService;
  @Mock private NextGenService nextGenService;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(cvSetupService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(cvSetupService, "kubernetesActivitySourceService", kubernetesActivitySourceService, true);
    FieldUtils.writeField(cvSetupService, "verificationJobService", verificationJobService, true);
    FieldUtils.writeField(cvSetupService, "cvConfigService", cvConfigService, true);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testgetSetupStatus() {
    String accountId = "accountId";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    when(nextGenService.getServicesCount(eq(accountId), eq(orgIdentifier), eq(projectIdentifier))).thenReturn(10);
    when(nextGenService.getEnvironmentCount(eq(accountId), eq(orgIdentifier), eq(projectIdentifier))).thenReturn(5);
    when(cvConfigService.getNumberOfServicesSetup(eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(5);
    when(kubernetesActivitySourceService.getNumberOfKubernetesServicesSetup(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(4);
    when(verificationJobService.getNumberOfServicesUndergoingHealthVerification(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(3);
    when(verificationJobService.doesAVerificationJobExistsForThisProject(
             eq(accountId), eq(orgIdentifier), eq(projectIdentifier)))
        .thenReturn(true);
    CVSetupStatusDTO cvSetupStatus = cvSetupService.getSetupStatus(accountId, orgIdentifier, projectIdentifier);
    assertThat(cvSetupStatus).isNotNull();
    assertThat(cvSetupStatus.getStepsWhichAreCompleted())
        .isEqualTo(Arrays.asList(ACTIVITY_SOURCE, MONITORING_SOURCE, VERIFICATION_JOBS));
    assertThat(cvSetupStatus.getTotalNumberOfServices()).isEqualTo(10);
    assertThat(cvSetupStatus.getTotalNumberOfEnvironments()).isEqualTo(5);
    assertThat(cvSetupStatus.getNumberOfServicesUsedInMonitoringSources()).isEqualTo(5);
    assertThat(cvSetupStatus.getNumberOfServicesUsedInActivitySources()).isEqualTo(4);
    assertThat(cvSetupStatus.getServicesUndergoingHealthVerification()).isEqualTo(3);
  }
}
