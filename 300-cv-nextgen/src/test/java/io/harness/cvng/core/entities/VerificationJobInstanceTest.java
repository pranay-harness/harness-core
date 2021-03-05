package io.harness.cvng.core.entities;

import static io.harness.cvng.core.services.CVNextGenConstants.DATA_COLLECTION_DELAY;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.rule.Owner;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VerificationJobInstanceTest extends CategoryTest {
  private CanaryVerificationJob verificationJob;
  private String accountId;
  private String monitoringSourceIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;

  @Before
  public void setup() {
    verificationJob = new CanaryVerificationJob();
    verificationJob.setDuration(Duration.ofMinutes(10));
    this.accountId = generateUuid();
    this.orgIdentifier = generateUuid();
    this.projectIdentifier = generateUuid();
    this.monitoringSourceIdentifier = generateUuid();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuilder_deploymentStartTimeRoundDown() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:02:06Z");
    VerificationJobInstance verificationJobInstance = VerificationJobInstance.builder()
                                                          .deploymentStartTime(deploymentStartTime)
                                                          .startTime(deploymentStartTime.plus(Duration.ofMinutes(2)))
                                                          .build();
    assertThat(verificationJobInstance.getDeploymentStartTime()).isEqualTo(Instant.parse("2020-04-22T10:02:00Z"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuilder_startTimeAndDeploymentStartTimeAreInSameMinute() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:02:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:02:58Z");
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder().deploymentStartTime(deploymentStartTime).startTime(startTime).build();
    assertThat(verificationJobInstance.getDeploymentStartTime()).isEqualTo(Instant.parse("2020-04-22T10:02:00Z"));
    assertThat(verificationJobInstance.getStartTime()).isEqualTo(Instant.parse("2020-04-22T10:03:00Z"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuilder_startTimeAndDeploymentStartTimeAreDifferentMinute() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:02:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:58Z");
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder().deploymentStartTime(deploymentStartTime).startTime(startTime).build();
    assertThat(verificationJobInstance.getDeploymentStartTime()).isEqualTo(Instant.parse("2020-04-22T10:02:00Z"));
    assertThat(verificationJobInstance.getStartTime()).isEqualTo(Instant.parse("2020-04-22T10:03:00Z"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  @Ignore("Enable once health verification start time is consistent with actual start time.")
  public void testBuilder_startTimeIsBeforeDeploymentStartTime() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:58Z");
    assertThatThrownBy(
        () -> VerificationJobInstance.builder().deploymentStartTime(deploymentStartTime).startTime(startTime).build())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Deployment start time should be before verification start time.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuilder_setDefaultDataCollectionDelay() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:58Z");
    assertThat(VerificationJobInstance.builder()
                   .deploymentStartTime(deploymentStartTime)
                   .startTime(startTime)
                   .build()
                   .getDataCollectionDelay())
        .isEqualTo(DATA_COLLECTION_DELAY);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProgressPercentage_emptyProgressLogs() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:58Z");
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder().deploymentStartTime(deploymentStartTime).startTime(startTime).build();
    assertThat(verificationJobInstance.getProgressPercentage()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRemainingTime_emptyProgressLogs() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:58Z");
    VerificationJobInstance verificationJobInstance = VerificationJobInstance.builder()
                                                          .deploymentStartTime(deploymentStartTime)
                                                          .startTime(startTime)
                                                          .resolvedJob(verificationJob)
                                                          .build();
    assertThat(verificationJobInstance.getRemainingTime(Instant.now())).isEqualTo(Duration.ofMinutes(15));
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProgressPercentage_withProgressLog() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:00Z");
    VerificationJobInstance.ProgressLog progressLog = VerificationJobInstance.AnalysisProgressLog.builder()
                                                          .startTime(startTime)
                                                          .endTime(startTime.plus(Duration.ofMinutes(1)))
                                                          .verificationTaskId(generateUuid())
                                                          .log("log")
                                                          .isFinalState(true)
                                                          .build();
    CVConfig cvConfig = newCVConfig();
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .deploymentStartTime(deploymentStartTime)
            .startTime(startTime)
            .progressLogs(Arrays.asList(progressLog))
            .cvConfigMap(Collections.singletonMap(cvConfig.getUuid(), cvConfig))
            .resolvedJob(verificationJob)
            .build();
    assertThat(verificationJobInstance.getProgressPercentage()).isEqualTo(10);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProgressPercentage_withMultipleVerificationTasks() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:00Z");
    VerificationJobInstance.ProgressLog progressLog = VerificationJobInstance.AnalysisProgressLog.builder()
                                                          .startTime(startTime)
                                                          .endTime(startTime.plus(Duration.ofMinutes(1)))
                                                          .verificationTaskId(generateUuid())
                                                          .log("log")
                                                          .isFinalState(true)
                                                          .build();
    Map<String, CVConfig> cvConfigMap = new HashMap<>();
    CVConfig cvConfig1 = newCVConfig();
    CVConfig cvConfig2 = newCVConfig();
    cvConfigMap.put(cvConfig1.getUuid(), cvConfig1);
    cvConfigMap.put(cvConfig2.getUuid(), cvConfig2);
    VerificationJobInstance verificationJobInstance = VerificationJobInstance.builder()
                                                          .deploymentStartTime(deploymentStartTime)
                                                          .startTime(startTime)
                                                          .progressLogs(Arrays.asList(progressLog))
                                                          .cvConfigMap(cvConfigMap)
                                                          .resolvedJob(verificationJob)
                                                          .build();
    assertThat(verificationJobInstance.getProgressPercentage()).isEqualTo(5);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetRemainingTime_withProgressLog() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:00Z");
    VerificationJobInstance.ProgressLog progressLog = VerificationJobInstance.AnalysisProgressLog.builder()
                                                          .startTime(startTime)
                                                          .endTime(startTime.plus(Duration.ofMinutes(1)))
                                                          .verificationTaskId(generateUuid())
                                                          .log("log")
                                                          .isFinalState(true)
                                                          .build();
    CVConfig cvConfig = newCVConfig();
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .deploymentStartTime(deploymentStartTime)
            .startTime(startTime)
            .progressLogs(Arrays.asList(progressLog))
            .cvConfigMap(Collections.singletonMap(cvConfig.getUuid(), cvConfig))
            .resolvedJob(verificationJob)
            .build();
    assertThat(verificationJobInstance.getRemainingTime(Instant.parse("2020-04-22T10:07:00Z")))
        .isEqualTo(Duration.ofMinutes(18));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRemainingTime_withMultipleVerificationTasks() {
    Instant deploymentStartTime = Instant.parse("2020-04-22T10:04:06Z");
    Instant startTime = Instant.parse("2020-04-22T10:03:00Z");
    VerificationJobInstance.ProgressLog progressLog = VerificationJobInstance.AnalysisProgressLog.builder()
                                                          .startTime(startTime)
                                                          .endTime(startTime.plus(Duration.ofMinutes(1)))
                                                          .verificationTaskId(generateUuid())
                                                          .log("log")
                                                          .isFinalState(true)
                                                          .build();
    Map<String, CVConfig> cvConfigMap = new HashMap<>();
    CVConfig cvConfig1 = newCVConfig();
    CVConfig cvConfig2 = newCVConfig();
    cvConfigMap.put(cvConfig1.getUuid(), cvConfig1);
    cvConfigMap.put(cvConfig2.getUuid(), cvConfig2);
    VerificationJobInstance verificationJobInstance = VerificationJobInstance.builder()
                                                          .deploymentStartTime(deploymentStartTime)
                                                          .startTime(startTime)
                                                          .progressLogs(Arrays.asList(progressLog))
                                                          .cvConfigMap(cvConfigMap)
                                                          .resolvedJob(verificationJob)
                                                          .build();
    assertThat(verificationJobInstance.getRemainingTime(Instant.parse("2020-04-22T10:06:00Z")))
        .isEqualTo(Duration.ofMinutes(19));
  }

  private CVConfig newCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setUuid(generateUuid());
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("serviceInstanceIdentifier");
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setIdentifier(monitoringSourceIdentifier);
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName("productName");
    return cvConfig;
  }
}
