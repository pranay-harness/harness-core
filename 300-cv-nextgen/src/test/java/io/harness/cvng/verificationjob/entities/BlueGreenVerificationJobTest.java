package io.harness.cvng.verificationjob.entities;

import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BlueGreenVerificationJobTest extends CategoryTest {
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
}
