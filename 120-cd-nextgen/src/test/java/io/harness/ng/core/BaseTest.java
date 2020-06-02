package io.harness.ng.core;

import io.harness.CategoryTest;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class BaseTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CoreTestRule coreTestRule = new CoreTestRule();
}