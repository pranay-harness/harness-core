package io.harness.resourcegroup;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(PL)
public class ResourceGroupTestBase extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule
  public ResourceGroupTestRule resourceGroupTestRule = new ResourceGroupTestRule(lifecycleRule.getClosingFactory());
}
