package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.NGCommonsRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class NGCommonsBaseTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public NGCommonsRule commonsRule = new NGCommonsRule(lifecycleRule.getClosingFactory());
}