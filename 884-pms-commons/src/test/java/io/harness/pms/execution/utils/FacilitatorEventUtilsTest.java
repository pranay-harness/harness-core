package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitatorEventUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testObtainLogContext() {
    try (AutoLogContext ignored = FacilitatorEventUtils.obtainLogContext(
             FacilitatorEvent.newBuilder()
                 .setNodeExecutionId("nodeid")
                 .setNotifyId("nid")
                 .setAmbiance(Ambiance.newBuilder().putSetupAbstractions("k", "v").build())
                 .build())) {
      assertThat(MDC.get("nodeExecutionId")).isEqualTo("nodeid");
      assertThat(MDC.get("notifyId")).isEqualTo("nid");
      assertThat(MDC.get("k")).isEqualTo("v");
    }
  }
}
