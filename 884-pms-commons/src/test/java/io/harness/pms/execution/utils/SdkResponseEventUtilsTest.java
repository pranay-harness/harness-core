package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

@OwnedBy(HarnessTeam.PIPELINE)
public class SdkResponseEventUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testObtainLogContext() {
    try (AutoLogContext ignored = SdkResponseEventUtils.obtainLogContext(
             SdkResponseEventProto.newBuilder()
                 .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
                 .setNodeExecutionId("nid")
                 .build())) {
      assertThat(MDC.get("sdkResponseEventType")).isEqualTo("ADD_EXECUTABLE_RESPONSE");
      assertThat(MDC.get("nodeExecutionId")).isEqualTo("nid");
    }
  }
}
