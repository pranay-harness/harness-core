package io.harness;

import io.harness.rule.EventServiceRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class EventServerTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public EventServiceRule apiServiceRule = new EventServiceRule(lifecycleRule.getClosingFactory());
}
