package software.wings.utils;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class ValidatorTest extends WingsBaseTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testStringTypeCheck() {
    assertThatThrownBy(() -> Validator.ensureType(String.class, 1, "Not of string type"));
    Validator.ensureType(String.class, "abc", "Not of string type");
    Validator.ensureType(Integer.class, 1, "Not of integer type");
  }
}
