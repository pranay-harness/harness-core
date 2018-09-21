package io.harness.rule;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CategoryTimeoutRuleTest extends CategoryTest {
  @Test
  @Category({UnitTests.class, CategoryTimeoutRule.RunMode.class})
  public void TestTheTimeoutCapability() {}
}
