package io.harness.cvng.verificationjob.entities;

import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class CanaryVerificationJobTest extends CategoryTest {
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
    Optional<TimeRange> timeRange = canaryVerificationJob.getPreDeploymentTimeRange(deploymentStartTime);
    assertThat(timeRange.isPresent()).isTrue();
    assertThat(Duration.between(timeRange.get().getStartTime(), timeRange.get().getEndTime()).toMinutes())
        .isEqualTo(canaryVerificationJob.getDuration().toMinutes());
  }

  private CanaryVerificationJob createCanaryVerificationJob() {
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    canaryVerificationJob.setDuration(Duration.ofMinutes(10));
    return canaryVerificationJob;
  }
}
