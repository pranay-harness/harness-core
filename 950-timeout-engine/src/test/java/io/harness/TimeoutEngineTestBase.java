package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.TimeoutEngineRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class TimeoutEngineTestBase extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public TimeoutEngineRule timeoutEngineRule = new TimeoutEngineRule(lifecycleRule.getClosingFactory());
}
