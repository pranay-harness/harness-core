package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class ValidatorTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testStringTypeCheck() {
    assertThatThrownBy(() -> Validator.ensureType(String.class, 1, "Not of string type"));
    Validator.ensureType(String.class, "abc", "Not of string type");
    Validator.ensureType(Integer.class, 1, "Not of integer type");
  }
}
