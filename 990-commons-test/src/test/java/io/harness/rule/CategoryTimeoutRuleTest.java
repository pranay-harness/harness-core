package io.harness.rule;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CategoryTimeoutRuleTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category({UnitTests.class, CategoryTimeoutRule.RunMode.class})
  public void testTheTimeoutCapability() {}
}
