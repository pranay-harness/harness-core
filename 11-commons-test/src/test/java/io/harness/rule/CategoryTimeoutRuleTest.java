package io.harness.rule;

import static io.harness.rule.OwnerRule.UNKNOWN;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CategoryTimeoutRuleTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category({UnitTests.class, CategoryTimeoutRule.RunMode.class})
  public void testTheTimeoutCapability() {}
}
