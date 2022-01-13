/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.verificationjob.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.job.BlueGreenVerificationJobDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.rule.Owner;

import java.time.Duration;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BlueGreenVerificationJobTest extends CategoryTest {
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
    BlueGreenVerificationJob blueGreenVerificationJob = createBlueGreenVerificationJob();
    blueGreenVerificationJob.setSensitivity(null);
    assertThatThrownBy(() -> blueGreenVerificationJob.validateParams())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("sensitivity should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category({UnitTests.class})
  public void validateParams_whenTrafficSplitPercentageIsLessThanZero() {
    BlueGreenVerificationJob blueGreenVerificationJob = createBlueGreenVerificationJob();
    blueGreenVerificationJob.setTrafficSplitPercentage(-5);
    assertThatThrownBy(() -> blueGreenVerificationJob.validateParams())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("trafficSplitPercentage is not in appropriate range");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void testFromDTO() {
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO = newBlueGreenVerificationJobDTO();
    BlueGreenVerificationJob blueGreenVerificationJob = new BlueGreenVerificationJob();
    blueGreenVerificationJob.fromDTO(blueGreenVerificationJobDTO);
    assertThat(blueGreenVerificationJob.getTrafficSplitPercentageV2().isRuntimeParam).isEqualTo(false);
    assertThat(blueGreenVerificationJob.getTrafficSplitPercentageV2().value).isEqualTo("10");
    assertThat(blueGreenVerificationJob.getSensitivity()).isEqualTo(Sensitivity.MEDIUM);
    assertThat(blueGreenVerificationJob.getIdentifier()).isEqualTo(verificationJobIdentifier);
    assertThat(blueGreenVerificationJob.getJobName()).isEqualTo(jobName);
    assertThat(blueGreenVerificationJob.getMonitoringSources()).hasSize(1);
    assertThat(blueGreenVerificationJob.getMonitoringSources().get(0)).isEqualTo("monitoringSourceIdentifier");
    assertThat(blueGreenVerificationJob.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(blueGreenVerificationJob.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(blueGreenVerificationJob.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(blueGreenVerificationJob.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(blueGreenVerificationJob.getDuration()).isEqualTo(Duration.ofMinutes(15));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void testGetVerificationJobDTO() {
    BlueGreenVerificationJob blueGreenVerificationJob = newBlueGreenVerificationJob();
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO =
        (BlueGreenVerificationJobDTO) blueGreenVerificationJob.getVerificationJobDTO();
    assertThat(blueGreenVerificationJobDTO.getTrafficSplitPercentage()).isEqualTo("10");
    assertThat(blueGreenVerificationJobDTO.getSensitivity()).isEqualTo("MEDIUM");
    assertThat(blueGreenVerificationJobDTO.getIdentifier()).isEqualTo(verificationJobIdentifier);
    assertThat(blueGreenVerificationJobDTO.getJobName()).isEqualTo(jobName);
    assertThat(blueGreenVerificationJobDTO.getMonitoringSources()).hasSize(1);
    assertThat(blueGreenVerificationJobDTO.getMonitoringSources().get(0)).isEqualTo("monitoringSourceIdentifier");
    assertThat(blueGreenVerificationJobDTO.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(blueGreenVerificationJobDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(blueGreenVerificationJobDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(blueGreenVerificationJobDTO.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(blueGreenVerificationJobDTO.getDuration()).isEqualTo("15m");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void testFromDTO_trafficSplitPercentageRunTimeParameter() {
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO = newBlueGreenVerificationJobDTO();
    blueGreenVerificationJobDTO.setTrafficSplitPercentage("<+input>");
    BlueGreenVerificationJob blueGreenVerificationJob = new BlueGreenVerificationJob();
    blueGreenVerificationJob.fromDTO(blueGreenVerificationJobDTO);
    assertThat(blueGreenVerificationJob.getTrafficSplitPercentageV2().isRuntimeParam).isEqualTo(true);
    assertThat(blueGreenVerificationJob.getTrafficSplitPercentageV2().value).isEqualTo("<+input>");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category({UnitTests.class})
  public void testGetVerificationJobDTO_trafficSplitPercentageRunTimeParameter() {
    BlueGreenVerificationJob blueGreenVerificationJob = newBlueGreenVerificationJob();
    blueGreenVerificationJob.setTrafficSplitPercentageV2("<+input>", true);
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO =
        (BlueGreenVerificationJobDTO) blueGreenVerificationJob.getVerificationJobDTO();
    assertThat(blueGreenVerificationJobDTO.getTrafficSplitPercentage()).isEqualTo("<+input>");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category({UnitTests.class})
  public void validateParams_whenTrafficSplitPercentageIsGreaterThanOneHundred() {
    BlueGreenVerificationJob blueGreenVerificationJob = createBlueGreenVerificationJob();
    blueGreenVerificationJob.setTrafficSplitPercentage(120);
    assertThatThrownBy(() -> blueGreenVerificationJob.validateParams())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("trafficSplitPercentage is not in appropriate range");
  }

  private BlueGreenVerificationJob createBlueGreenVerificationJob() {
    BlueGreenVerificationJob blueGreenVerificationJob = new BlueGreenVerificationJob();
    blueGreenVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    blueGreenVerificationJob.setTrafficSplitPercentage(50);
    return blueGreenVerificationJob;
  }

  private BlueGreenVerificationJobDTO newBlueGreenVerificationJobDTO() {
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO = new BlueGreenVerificationJobDTO();
    blueGreenVerificationJobDTO.setIdentifier(verificationJobIdentifier);
    blueGreenVerificationJobDTO.setJobName(jobName);
    blueGreenVerificationJobDTO.setMonitoringSources(Arrays.asList("monitoringSourceIdentifier"));
    blueGreenVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    blueGreenVerificationJobDTO.setServiceIdentifier(serviceIdentifier);
    blueGreenVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    blueGreenVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    blueGreenVerificationJobDTO.setEnvIdentifier(envIdentifier);
    blueGreenVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    blueGreenVerificationJobDTO.setDuration("15m");
    blueGreenVerificationJobDTO.setTrafficSplitPercentage("10");
    return blueGreenVerificationJobDTO;
  }

  private BlueGreenVerificationJob newBlueGreenVerificationJob() {
    BlueGreenVerificationJob blueGreenVerificationJob = new BlueGreenVerificationJob();
    blueGreenVerificationJob.fromDTO(newBlueGreenVerificationJobDTO());
    return blueGreenVerificationJob;
  }
}
