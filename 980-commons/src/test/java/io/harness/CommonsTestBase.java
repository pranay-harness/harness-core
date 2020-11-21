package io.harness;

import io.harness.rule.CommonsRule;
import io.harness.rule.LifecycleRule;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class CommonsTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CommonsRule commonsRule = new CommonsRule(lifecycleRule.getClosingFactory());
}
