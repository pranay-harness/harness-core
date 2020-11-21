package io.harness.cvng;

import io.harness.rule.CvNextGenRule;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class HoverflyCVNextGenTest extends HoverflyTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CvNextGenRule cvNextGenRule = new CvNextGenRule(lifecycleRule.getClosingFactory());
}
