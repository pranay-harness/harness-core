package io.harness;

import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
public abstract class OrchestrationTest extends CategoryTest implements MockableTestMixin {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
}