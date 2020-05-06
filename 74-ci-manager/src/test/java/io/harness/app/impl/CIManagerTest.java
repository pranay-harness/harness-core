package io.harness.app.impl;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class CIManagerTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public CIManagerRule orchestrationRule = new CIManagerRule(lifecycleRule.getClosingFactory());
}