package io.harness.cvng.core.entities;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.rule.Owner;

import java.time.Duration;
import java.time.Instant;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VerificationJobInstanceTest extends CategoryTest {
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
}
