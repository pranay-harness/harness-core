package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.NgManagerRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class NgManagerTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public NgManagerRule apiServiceRule = new NgManagerRule(lifecycleRule.getClosingFactory());
}