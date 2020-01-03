package io.harness.validation;

import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.validation.Validator.ensureType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ValidatorTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testStringTypeCheck() {
    assertThatThrownBy(() -> ensureType(String.class, 1, "Not of string type"));
    ensureType(String.class, "abc", "Not of string type");
    ensureType(Integer.class, 1, "Not of integer type");
  }
}
