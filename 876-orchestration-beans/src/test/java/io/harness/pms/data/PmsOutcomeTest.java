package io.harness.pms.data;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationBeansTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsOutcomeTest extends OrchestrationBeansTestBase {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testConvertJsonToOrchestrationMap() {
    Map<String, String> jsons = new HashMap<>();
    jsons.put("key", "test");
    assertThat(PmsOutcome.convertJsonToOrchestrationMap(jsons)).isNotNull();
    assertThat(PmsOutcome.convertJsonToOrchestrationMap(jsons).size()).isEqualTo(1);
  }
}