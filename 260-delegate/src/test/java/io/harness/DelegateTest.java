package io.harness;

import io.harness.rule.DelegateRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class DelegateTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public DelegateRule delegateRule = new DelegateRule(lifecycleRule.getClosingFactory());
}
