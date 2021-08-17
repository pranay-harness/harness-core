package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnsupportedOperationException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class ApprovalStatusTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testStatuses() {
    for (ApprovalStatus status : ApprovalStatus.values()) {
      if (status.isFinalStatus()) {
        assertThat(status.toFinalExecutionStatus()).isNotNull();
      } else {
        assertThatThrownBy(() -> status.toFinalExecutionStatus()).isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }
}
