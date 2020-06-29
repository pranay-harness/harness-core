package io.harness;

import io.harness.rule.EventServerRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class EventServerTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public EventServerRule apiServiceRule = new EventServerRule(lifecycleRule.getClosingFactory());
}