package io.harness.pms.sdk.core.interrupt;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class InterruptEventHandlerTest extends PmsSdkCoreTestBase {
  @Mock private PMSInterruptService pmsInterruptService;
  @Mock private StepRegistry stepRegistry;

  @InjectMocks InterruptEventHandler interruptEventHandler;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractLogProperties() {
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(AmbianceTestUtils.buildAmbiance())
                               .setType(InterruptType.ABORT_ALL)
                               .setInterruptUuid("interruptUuid")
                               .build();
    Map<String, String> autoLogMap = ImmutableMap.<String, String>builder()
                                         .put("interruptType", event.getType().name())
                                         .put("interruptUuid", event.getInterruptUuid())
                                         .put("notifyId", event.getNotifyId())
                                         .build();
    assertThat(interruptEventHandler.extraLogProperties(event)).isEqualTo(autoLogMap);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractAmbiance() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    InterruptEvent event = InterruptEvent.newBuilder()
                               .setAmbiance(ambiance)
                               .setType(InterruptType.ABORT_ALL)
                               .setInterruptUuid("interruptUuid")
                               .build();
    assertThat(interruptEventHandler.extractAmbiance(event)).isEqualTo(ambiance);
  }
}