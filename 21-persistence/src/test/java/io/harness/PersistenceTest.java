package io.harness;

import io.harness.rule.LifecycleRule;
import io.harness.rule.PersistenceRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class PersistenceTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public PersistenceRule persistenceRule = new PersistenceRule(lifecycleRule.getClosingFactory());
}