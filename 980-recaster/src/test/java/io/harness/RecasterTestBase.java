package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.RecasterRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class RecasterTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public RecasterRule recasterRule = new RecasterRule(lifecycleRule.getClosingFactory());
}
