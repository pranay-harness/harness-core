package io.harness;

import io.harness.rule.DelegateTasksRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class DelegateTasksTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public DelegateTasksRule delegateTasksRule = new DelegateTasksRule(lifecycleRule.getClosingFactory());
}
