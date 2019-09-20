package io.harness.migrator;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.migrator.rule.MigratorRule;
import io.harness.rule.LifecycleRule;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class MigratorTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public MigratorRule orchestrationRule = new MigratorRule(lifecycleRule.getClosingFactory());
}