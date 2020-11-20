package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.NgManagerRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
public abstract class NgManagerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public NgManagerRule apiServiceRule = new NgManagerRule(lifecycleRule.getClosingFactory());
}
