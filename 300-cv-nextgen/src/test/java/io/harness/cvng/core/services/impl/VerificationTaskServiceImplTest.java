/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.impl;

import static io.harness.cvng.beans.DataSourceType.APP_DYNAMICS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VerificationTaskServiceImplTest extends CvNextGenTestBase {
  @Inject private VerificationTaskService verificationTaskService;
  private String accountId;

  @Before
  public void setup() {
    accountId = generateUuid();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_cvConfig() {
    String cvConfigId = generateUuid();
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, APP_DYNAMICS);
    VerificationTask verificationTask = verificationTaskService.get(verificationTaskId);
    assertThat(verificationTask.getValidUntil()).isNull();
    assertThat(verificationTaskService.getCVConfigId(verificationTaskId)).isEqualTo(cvConfigId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_cvConfigNull() {
    assertThatThrownBy(() -> verificationTaskService.getCVConfigId(generateUuid()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Invalid verificationTaskId. Verification mapping does not exist.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_verificationTask() throws IllegalAccessException {
    Clock clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(verificationTaskService, "clock", clock, true);
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    VerificationTask verificationTask = verificationTaskService.get(verificationTaskId);
    assertThat(verificationTask.getValidUntil()).isEqualTo(Date.from(Instant.parse("2020-05-22T10:02:06Z")));
    assertThat(verificationTaskService.getCVConfigId(verificationTaskId)).isEqualTo(cvConfigId);
    assertThat(verificationTaskService.getVerificationJobInstanceId(verificationTaskId))
        .isEqualTo(verificationJobInstanceId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_verificationTaskIsNull() {
    assertThatThrownBy(() -> verificationTaskService.getVerificationJobInstanceId(generateUuid()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Invalid verificationTaskId. Verification mapping does not exist.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationTaskIds() {
    String verificationJobInstanceId = generateUuid();

    Set<String> cvConfigIds = new HashSet<>();
    Set<String> verificationTaskIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String cvConfigId = generateUuid();
      String verificationTaskId =
          verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
      cvConfigIds.add(cvConfigId);
      verificationTaskIds.add(verificationTaskId);
    }
    assertThat(verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
        .isEqualTo(verificationTaskIds);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetServiceGuardVerificationTaskId_ifExist() {
    String cvConfigId = generateUuid();
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, APP_DYNAMICS);
    assertThat(verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId))
        .isEqualTo(verificationTaskId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetServiceGuardVerificationTaskId_ifDoesNotExist() {
    String cvConfigId = generateUuid();
    assertThatThrownBy(() -> verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage(
            "VerificationTask mapping does not exist for cvConfigId " + cvConfigId + ". Please check cvConfigId");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testIsServiceGuardId_ifExist() {
    String cvConfigId = generateUuid();
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, APP_DYNAMICS);
    assertThat(verificationTaskService.isServiceGuardId(verificationTaskId)).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testIsServiceGuardId_ifDoesNotExist() {
    String verificationTaskId = generateUuid();
    assertThatThrownBy(() -> verificationTaskService.isServiceGuardId(verificationTaskId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Invalid verificationTaskId. Verification mapping does not exist.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testIsServiceGuardId_ifExistDeployment() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String verificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    assertThat(verificationTaskService.isServiceGuardId(verificationTaskId)).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRemoveCVConfigMapping_multipleServiceGuardAndDeploymentMappings() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String verificationTaskId1 =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    String verificationTaskId2 = verificationTaskService.create(accountId, cvConfigId, APP_DYNAMICS);
    assertThat(verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId))
        .isEqualTo(verificationTaskId2);
    assertThat(verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
        .isEqualTo(Sets.newHashSet(verificationTaskId1));
    verificationTaskService.removeCVConfigMappings(cvConfigId);
    assertThatThrownBy(() -> verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage(
            "VerificationTask mapping does not exist for cvConfigId " + cvConfigId + ". Please check cvConfigId");
    assertThat(verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId)).hasSize(1);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetAllVerificationJobInstanceIdsForCVConfig() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId1 = generateUuid();
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId1, APP_DYNAMICS);
    verificationTaskService.create(accountId, cvConfigId, APP_DYNAMICS);
    String verificationJobInstanceId2 = generateUuid();
    verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId2, APP_DYNAMICS);
    assertThat(new HashSet<>(verificationTaskService.getAllVerificationJobInstanceIdsForCVConfig(cvConfigId)))
        .isEqualTo(Sets.newHashSet(verificationJobInstanceId1, verificationJobInstanceId2));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFindBaselineVerificationTaskId_baselineVerificationTaskIdExists() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String currentVerificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    String baselineVerificationJobInstanceId = generateUuid();
    String baselineVerificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, baselineVerificationJobInstanceId, APP_DYNAMICS);
    Optional<String> result = verificationTaskService.findBaselineVerificationTaskId(currentVerificationTaskId,
        VerificationJobInstance.builder()
            .accountId(accountId)
            .startTime(Instant.now())
            .deploymentStartTime(Instant.now())
            .resolvedJob(TestVerificationJob.builder()
                             .accountId(accountId)
                             .baselineVerificationJobInstanceId(baselineVerificationJobInstanceId)
                             .build())
            .build());
    assertThat(result.get()).isEqualTo(baselineVerificationTaskId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFindBaselineVerificationTaskId_baselineVerificationTaskIdDoesNotExists() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String currentVerificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    String baselineVerificationJobInstanceId = generateUuid();

    Optional<String> result = verificationTaskService.findBaselineVerificationTaskId(currentVerificationTaskId,
        VerificationJobInstance.builder()
            .startTime(Instant.now())
            .deploymentStartTime(Instant.now())
            .resolvedJob(TestVerificationJob.builder()
                             .baselineVerificationJobInstanceId(baselineVerificationJobInstanceId)
                             .build())
            .build());
    assertThat(result).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFindBaselineVerificationTaskId_baselineVerificationJobInstanceIdDoesNotExist() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String currentVerificationTaskId =
        verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId, APP_DYNAMICS);
    Optional<String> result = verificationTaskService.findBaselineVerificationTaskId(currentVerificationTaskId,
        VerificationJobInstance.builder()
            .startTime(Instant.now())
            .deploymentStartTime(Instant.now())
            .resolvedJob(TestVerificationJob.builder().build())
            .build());
    assertThat(result).isEmpty();
  }
}
