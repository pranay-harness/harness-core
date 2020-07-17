package io.harness.cvng.verificationjob.entities;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

  private CanaryVerificationJob createCanaryVerificationJob() {
    CanaryVerificationJob canaryVerificationJob = new CanaryVerificationJob();
    canaryVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    return canaryVerificationJob;
  }
}
