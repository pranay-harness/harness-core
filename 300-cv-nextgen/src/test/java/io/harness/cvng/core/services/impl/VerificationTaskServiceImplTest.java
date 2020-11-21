package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VerificationTaskServiceImplTest extends CvNextGenTest {
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
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId);
    VerificationTask verificationTask = verificationTaskService.get(verificationTaskId);
    assertThat(verificationTask.getValidUntil()).isNull();
    assertThat(verificationTaskService.getCVConfigId(verificationTaskId)).isEqualTo(cvConfigId);
  }

  @Test()
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
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
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
      String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
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
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId);
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
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId);
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
    String verificationTaskId = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    assertThat(verificationTaskService.isServiceGuardId(verificationTaskId)).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRemoveCVConfigMapping_multipleServiceGuardAndDeploymentMappings() {
    String cvConfigId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    String verificationTaskId1 = verificationTaskService.create(accountId, cvConfigId, verificationJobInstanceId);
    String verificationTaskId2 = verificationTaskService.create(accountId, cvConfigId);
    assertThat(verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId))
        .isEqualTo(verificationTaskId2);
    assertThat(verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
        .isEqualTo(Sets.newHashSet(verificationTaskId1));
    verificationTaskService.removeCVConfigMappings(cvConfigId);
    assertThatThrownBy(() -> verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId))
        .isInstanceOf(NullPointerException.class)
        .hasMessage(
            "VerificationTask mapping does not exist for cvConfigId " + cvConfigId + ". Please check cvConfigId");
    assertThatThrownBy(() -> verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No verification task mapping exist for verificationJobInstanceId " + verificationJobInstanceId);
  }
}
