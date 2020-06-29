package io.harness;

import io.harness.rule.CiBeansRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public class CiBeansTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CiBeansRule apiServiceRule = new CiBeansRule(lifecycleRule.getClosingFactory());
}