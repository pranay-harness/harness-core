package io.harness;

import io.harness.rule.DelegateServiceRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class DelegateServiceTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public DelegateServiceRule apiServiceRule = new DelegateServiceRule(lifecycleRule.getClosingFactory());
}
