package io.harness.cvng.verificationjob.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Optional;

public class TestVerificationJobTest extends CategoryTest {
  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidateParams() {
    TestVerificationJob testVerificationJob = createTestVerificationJob();
    testVerificationJob.validateParams();
    testVerificationJob.setSensitivity(null);
    assertThatThrownBy(() -> testVerificationJob.validateParams())
        .isInstanceOf(NullPointerException.class)
        .hasMessage("sensitivity should not be null");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testResolveAdditionsFields_emptyLastExecution() {
    TestVerificationJob testVerificationJob = createTestVerificationJob();
    assertThat(testVerificationJob.getBaselineVerificationJobInstanceId()).isNull();
    VerificationJobInstanceService verificationJobInstanceService = mock(VerificationJobInstanceService.class);
    when(verificationJobInstanceService.getLastSuccessfulTestVerificationJobExecutionId(any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    testVerificationJob.resolveAdditionsFields(verificationJobInstanceService);
    assertThat(testVerificationJob.getBaselineVerificationJobInstanceId()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testResolveAdditionsFields_withValidBaseline() {
    TestVerificationJob testVerificationJob = createTestVerificationJob();
    assertThat(testVerificationJob.getBaselineVerificationJobInstanceId()).isNull();
    VerificationJobInstanceService verificationJobInstanceService = mock(VerificationJobInstanceService.class);
    String baseline = generateUuid();
    when(verificationJobInstanceService.getLastSuccessfulTestVerificationJobExecutionId(any(), any(), any(), any()))
        .thenReturn(Optional.of(baseline));
    testVerificationJob.resolveAdditionsFields(verificationJobInstanceService);
    assertThat(testVerificationJob.getBaselineVerificationJobInstanceId()).isEqualTo(baseline);
  }

  private TestVerificationJob createTestVerificationJob() {
    TestVerificationJob testVerificationJob = new TestVerificationJob();
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    return testVerificationJob;
  }
}
