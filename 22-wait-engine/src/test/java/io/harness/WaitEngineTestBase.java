package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.WaitEngineRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class WaitEngineTestBase extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public WaitEngineRule waitEngineRule = new WaitEngineRule(lifecycleRule.getClosingFactory());
}
