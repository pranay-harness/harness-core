package io.harness.gitsync;

import io.harness.CategoryTest;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class GitSyncBaseTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GitSyncTestRule gitSyncTestRule = new GitSyncTestRule(lifecycleRule.getClosingFactory());
}
