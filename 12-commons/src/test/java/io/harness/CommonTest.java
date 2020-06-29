package io.harness;

import io.harness.rule.CommonRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class CommonTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CommonRule apiServiceRule = new CommonRule(lifecycleRule.getClosingFactory());
}