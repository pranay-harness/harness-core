package io.harness;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import io.harness.category.element.UnitTests;
import io.harness.rule.EventServiceRule;
import io.harness.rule.LifecycleRule;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public abstract class EventServerTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public EventServiceRule apiServiceRule = new EventServiceRule(lifecycleRule.getClosingFactory());

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  @Ignore("Placeholder")
  public void eventServerTest() {}
}
