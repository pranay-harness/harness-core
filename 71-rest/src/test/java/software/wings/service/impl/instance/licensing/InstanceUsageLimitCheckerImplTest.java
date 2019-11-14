package software.wings.service.impl.instance.licensing;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InstanceUsageLimitCheckerImplTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testIsWithinLimit() {
    double actualUsage = 90;
    double allowed = 100;
    boolean withinLimit = InstanceUsageLimitCheckerImpl.isWithinLimit(actualUsage, 85, allowed);
    assertThat(withinLimit).isFalse();
    withinLimit = InstanceUsageLimitCheckerImpl.isWithinLimit(actualUsage, 95, allowed);
    assertThat(withinLimit).isTrue();
  }
}