package io.harness.task;

import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public abstract class DelegateTaskGrpcBaseTest extends CategoryTest implements MockableTestMixin {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public TaskServiceRule taskServiceRule = new TaskServiceRule(lifecycleRule.getClosingFactory());
}
