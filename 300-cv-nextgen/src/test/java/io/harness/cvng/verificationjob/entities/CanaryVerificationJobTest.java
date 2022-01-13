/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.verificationjob.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.job.CanaryVerificationJobDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.rule.Owner;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CanaryVerificationJobTest extends CategoryTest {
  private String verificationJobIdentifier;
  private String jobName;
  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    verificationJobIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    jobName = generateUuid();
    envIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category({UnitTests.class})
  public void validateParams_whenSensitivityIsNull() {
    CanaryVerificationJob canaryVerificationJob = createCanaryVerificationJob();
    canaryVerificationJob.setSensitivity(null);
    assertThatThrownBy(() -> canaryVerificationJob.validateParams())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("sensitivity should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category({UnitTests.class})
  public void validateParams_whenTrafficSplitPercentageIsLessThanZero() {
    CanaryVerificationJob canaryVerificationJob = createCanaryVerificationJob();
    canaryVerificationJob.setTrafficSplitPercentage(-5);
    assertThatThrownBy(() -> canaryVerificationJob.validateParams())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("trafficSplitPercentage is not in appropriate range");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category({UnitTests.class})
  public void validateParams_whenTrafficSplitPercentageIsGreaterThanOneHundred() {
    CanaryVerificationJob canaryVerificationJob = createCanaryVerificationJob();
    canaryVerificationJob.setTrafficSplitPercentage(120);
    assertThatThrownBy(() -> canaryVerificationJob.validateParams())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("trafficSplitPercentage is not in appropriate range");
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category({UnitTests.class})
  public void testGetPreDeploymentTimeRange() {
    CanaryVerificationJob canaryVerificationJob = createCanaryVerificationJob();
    Instant deploymentStartTime = Instant.now();
    Optional<TimeRange> timeRange = canaryVerificationJob.getPreActivityTimeRange(deploymentStartTime);
    assertThat(timeRange.isPresent()).isTrue();
    assertThat(Duration.between(timeRange.get().getStartTime(), timeRange.get().getEndTime()).toMinutes())
        .isEqualTo(canaryVerificationJob.getDuration().toMinutes());
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void testFromDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = newCanaryVerificationJobDTO();
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.fromDTO(canaryVerificationJobDTO);
    assertThat(canaryVerificationJob.getTrafficSplitPercentageV2().isRuntimeParam).isEqualTo(false);
    assertThat(canaryVerificationJob.getTrafficSplitPercentageV2().value).isEqualTo("10");
    assertThat(canaryVerificationJob.getSensitivity()).isEqualTo(Sensitivity.MEDIUM);
    assertThat(canaryVerificationJob.getIdentifier()).isEqualTo(verificationJobIdentifier);
    assertThat(canaryVerificationJob.getJobName()).isEqualTo(jobName);
    assertThat(canaryVerificationJob.getMonitoringSources()).hasSize(1);
    assertThat(canaryVerificationJob.getMonitoringSources().get(0)).isEqualTo("monitoringSourceIdentifier");
    assertThat(canaryVerificationJob.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(canaryVerificationJob.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(canaryVerificationJob.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(canaryVerificationJob.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(canaryVerificationJob.getDuration()).isEqualTo(Duration.ofMinutes(15));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void testGetVerificationJobDTO() {
    CanaryVerificationJob canaryVerificationJob = newCanaryVerificationJob();
    CanaryVerificationJobDTO canaryVerificationJobDTO =
        (CanaryVerificationJobDTO) canaryVerificationJob.getVerificationJobDTO();
    assertThat(canaryVerificationJobDTO.getTrafficSplitPercentage()).isEqualTo("10");
    assertThat(canaryVerificationJobDTO.getSensitivity()).isEqualTo("MEDIUM");
    assertThat(canaryVerificationJobDTO.getIdentifier()).isEqualTo(verificationJobIdentifier);
    assertThat(canaryVerificationJobDTO.getJobName()).isEqualTo(jobName);
    assertThat(canaryVerificationJobDTO.getMonitoringSources()).hasSize(1);
    assertThat(canaryVerificationJobDTO.getMonitoringSources().get(0)).isEqualTo("monitoringSourceIdentifier");
    assertThat(canaryVerificationJobDTO.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(canaryVerificationJobDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(canaryVerificationJobDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(canaryVerificationJobDTO.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(canaryVerificationJobDTO.getDuration()).isEqualTo("15m");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testGetSensitivity() {
    CanaryVerificationJob canaryVerificationJob = createCanaryVerificationJob();
    canaryVerificationJob.setSensitivity("High", false);
    assertThat(canaryVerificationJob.getSensitivity()).isEqualTo(Sensitivity.HIGH);
    canaryVerificationJob.setSensitivity("HIGH", false);
    assertThat(canaryVerificationJob.getSensitivity()).isEqualTo(Sensitivity.HIGH);
    canaryVerificationJob.setSensitivity("HIgH", false);
    assertThatThrownBy(() -> canaryVerificationJob.getSensitivity())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No enum mapping found for HIgH");
  }

  private CanaryVerificationJob createCanaryVerificationJob() {
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    canaryVerificationJob.setDuration(Duration.ofMinutes(10));
    return canaryVerificationJob;
  }

  private CanaryVerificationJobDTO newCanaryVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(verificationJobIdentifier);
    canaryVerificationJobDTO.setJobName(jobName);
    canaryVerificationJobDTO.setMonitoringSources(Arrays.asList("monitoringSourceIdentifier"));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setServiceIdentifier(serviceIdentifier);
    canaryVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    canaryVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    canaryVerificationJobDTO.setEnvIdentifier(envIdentifier);
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setDuration("15m");
    canaryVerificationJobDTO.setTrafficSplitPercentage("10");
    return canaryVerificationJobDTO;
  }

  private CanaryVerificationJob newCanaryVerificationJob() {
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.fromDTO(newCanaryVerificationJobDTO());
    return canaryVerificationJob;
  }
}
