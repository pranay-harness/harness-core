package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.OrchestrationBeansRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class OrchestrationBeansTestBase extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public OrchestrationBeansRule orchestrationRule = new OrchestrationBeansRule(lifecycleRule.getClosingFactory());
}
