package io.harness.gitopsprovider;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(GITOPS)
public abstract class GitOpsProviderTestBase extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public GitOpsTestRule gitOpsProviderTestBase = new GitOpsTestRule(lifecycleRule.getClosingFactory());
}
