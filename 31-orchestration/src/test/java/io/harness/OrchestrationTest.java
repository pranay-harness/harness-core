package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.OrchestrationRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class OrchestrationTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public OrchestrationRule orchestrationRule = new OrchestrationRule(lifecycleRule.getClosingFactory());
}