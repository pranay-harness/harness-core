package io.harness.expression;

import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class ExpressionServiceTestBase {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule
  public ExpressionServiceRule expressionServiceRule = new ExpressionServiceRule(lifecycleRule.getClosingFactory());
}
