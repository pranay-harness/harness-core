package io.harness;

import static io.harness.rule.OwnerRule.DEEPAK;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class DashboardServiceStartupTest extends CategoryTest {
  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testAppStartup() {
    // todo @deepak: Add the logic for app startup
  }
}