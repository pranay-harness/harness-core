package io.harness;

import io.harness.rule.ApiServiceRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class ApiServiceTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public ApiServiceRule apiServiceRule = new ApiServiceRule(lifecycleRule.getClosingFactory());
}
