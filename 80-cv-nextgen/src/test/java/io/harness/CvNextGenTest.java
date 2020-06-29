package io.harness;

import io.harness.rule.CvNextGenRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class CvNextGenTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CvNextGenRule apiServiceRule = new CvNextGenRule(lifecycleRule.getClosingFactory());
}