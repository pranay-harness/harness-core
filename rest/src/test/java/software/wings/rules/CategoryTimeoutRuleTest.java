package software.wings.rules;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.BasicTest;
import software.wings.category.element.UnitTests;

public class CategoryTimeoutRuleTest extends BasicTest {
  @Test
  @Category({UnitTests.class, CategoryTimeoutRule.RunMode.class})
  public void TestTheTimeoutCapability() {}
}
