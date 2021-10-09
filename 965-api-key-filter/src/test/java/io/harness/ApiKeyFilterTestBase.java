package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rule.LifecycleRule;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
@OwnedBy(PL)
public class ApiKeyFilterTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public ApiKeyFilterRule commonsRule = new ApiKeyFilterRule(lifecycleRule.getClosingFactory());
}
