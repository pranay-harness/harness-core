package io.harness;

import io.harness.rule.CvNextGenCommonsRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class CvNextGenCommonsTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CvNextGenCommonsRule apiServiceRule = new CvNextGenCommonsRule(lifecycleRule.getClosingFactory());
}