package io.harness.data.algorithm;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class IdentifierNameTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testRandom() {
    String identifier = IdentifierName.random();
    assertThat(Character.isLetter(identifier.charAt(0))).isTrue();
  }
}
