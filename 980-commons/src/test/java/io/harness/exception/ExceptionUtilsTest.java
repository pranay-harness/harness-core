package io.harness.exception;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExceptionUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCause() {
    assertThat(ExceptionUtils.cause(Exception.class, null)).isNull();
    assertThat(ExceptionUtils.cause(Exception.class, new Exception())).isNotNull();

    assertThat(ExceptionUtils.cause(RuntimeException.class, new Exception())).isNull();
    assertThat(ExceptionUtils.cause(RuntimeException.class, new Exception(new RuntimeException()))).isNotNull();
  }
}