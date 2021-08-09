package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EngineExceptionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMapToWingsFailureType() {
    for (FailureType failureType : FailureType.values()) {
      io.harness.exception.FailureType wingsFailureType = EngineExceptionUtils.mapToWingsFailureType(failureType);
      assertThat(wingsFailureType).isNotNull();
    }
  }
}