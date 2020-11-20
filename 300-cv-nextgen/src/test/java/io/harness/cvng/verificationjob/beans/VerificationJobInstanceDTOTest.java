package io.harness.cvng.verificationjob.beans;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;

public class VerificationJobInstanceDTOTest extends CategoryTest {
  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testGetDeploymentStartTime_ifDeploymentTimeIsNotDefined() {
    VerificationJobInstanceDTO verificationJobInstanceDTO =
        VerificationJobInstanceDTO.builder()
            .verificationJobIdentifier("identifier")
            .verificationTaskStartTimeMs(Instant.parse("2020-04-22T10:02:06Z").toEpochMilli())
            .build();
    assertThat(verificationJobInstanceDTO.getVerificationStartTime()).isEqualTo(Instant.parse("2020-04-22T10:02:00Z"));
    assertThat(verificationJobInstanceDTO.getDeploymentStartTime()).isEqualTo(Instant.parse("2020-04-22T09:57:00Z"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testGetDeploymentStartTime_ifDeploymentTimeIsDefined() {
    VerificationJobInstanceDTO verificationJobInstanceDTO =
        VerificationJobInstanceDTO.builder()
            .verificationJobIdentifier("identifier")
            .verificationTaskStartTimeMs(Instant.parse("2020-04-22T10:02:06Z").toEpochMilli())
            .deploymentStartTimeMs(Instant.parse("2020-04-22T09:59:06Z").toEpochMilli())
            .build();
    assertThat(verificationJobInstanceDTO.getVerificationStartTime()).isEqualTo(Instant.parse("2020-04-22T10:02:00Z"));
    assertThat(verificationJobInstanceDTO.getDeploymentStartTime()).isEqualTo(Instant.parse("2020-04-22T09:59:00Z"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testGetVerificationStartTime() {
    VerificationJobInstanceDTO verificationJobInstanceDTO =
        VerificationJobInstanceDTO.builder()
            .verificationJobIdentifier("identifier")
            .verificationTaskStartTimeMs(Instant.parse("2020-04-22T10:02:06Z").toEpochMilli())
            .build();
    assertThat(verificationJobInstanceDTO.getVerificationStartTime()).isEqualTo(Instant.parse("2020-04-22T10:02:00Z"));
  }
}
