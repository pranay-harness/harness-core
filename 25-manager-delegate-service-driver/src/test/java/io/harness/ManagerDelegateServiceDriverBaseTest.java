package io.harness;

import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class ManagerDelegateServiceDriverBaseTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public ManagerDelegateServiceDriverRule delegateServiceDriverRule =
      new ManagerDelegateServiceDriverRule(lifecycleRule.getClosingFactory());
}
