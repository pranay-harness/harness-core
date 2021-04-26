package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(DX)
public abstract class NGMigrationTestBase extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public NGMigrationTestRule migrationTestRule = new NGMigrationTestRule(lifecycleRule.getClosingFactory());
}
