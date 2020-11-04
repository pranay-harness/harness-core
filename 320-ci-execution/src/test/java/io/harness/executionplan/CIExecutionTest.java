package io.harness.executionplan;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.executionplan.rule.CIExecutionRule;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class CIExecutionTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public CIExecutionRule executionRule = new CIExecutionRule(lifecycleRule.getClosingFactory());
}