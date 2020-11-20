package io.harness;

import io.harness.cvng.CVNextGenCommonTestRule;
import io.harness.rule.LifecycleRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
public abstract class CvNextGenCommonsTestBase extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public CVNextGenCommonTestRule cvNextGenCommonTestRule = new CVNextGenCommonTestRule();
}
