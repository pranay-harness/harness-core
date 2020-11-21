package io.harness.cdng;

import io.harness.CategoryTest;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class CDNGBaseTest extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public CDNGTestRule coreTestRule = new CDNGTestRule(lifecycleRule.getClosingFactory());
}
