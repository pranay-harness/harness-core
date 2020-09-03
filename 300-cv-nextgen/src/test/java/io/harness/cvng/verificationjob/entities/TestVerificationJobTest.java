package io.harness.cvng.verificationjob.entities;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestVerificationJobTest extends CategoryTest {
  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void validateParams() {
    TestVerificationJob testVerificationJob = createTestVerificationJob();
    testVerificationJob.validateParams();
    testVerificationJob.setSensitivity(null);
    assertThatThrownBy(() -> testVerificationJob.validateParams())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("sensitivity should not be null");
  }

  private TestVerificationJob createTestVerificationJob() {
    TestVerificationJob testVerificationJob = new TestVerificationJob();
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    return testVerificationJob;
  }
}