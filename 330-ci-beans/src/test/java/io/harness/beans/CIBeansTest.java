package io.harness.beans;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class CIBeansTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public CIBeansRule orchestrationRule = new CIBeansRule(lifecycleRule.getClosingFactory());
}