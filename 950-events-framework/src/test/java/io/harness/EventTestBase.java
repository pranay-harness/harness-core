package io.harness;

import io.harness.rule.EventRule;
import io.harness.rule.LifecycleRule;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class EventTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public EventRule apiServiceRule = new EventRule(lifecycleRule.getClosingFactory());
}
