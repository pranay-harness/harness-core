package io.harness.pms.sdk.core.execution.events.node.start;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeStartEventHandlerTest extends PmsSdkCoreTestBase {
  private static String NOTIFY_ID = "notifyId";

  @InjectMocks NodeStartEventHandler nodeStartEventHandler;

  private NodeStartEvent nodeStartEvent;
  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    nodeStartEvent = NodeStartEvent.newBuilder().setAmbiance(ambiance).setMode(ExecutionMode.APPROVAL).build();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractMetricContext() {
    Map<String, String> metricsMap = nodeStartEventHandler.extractMetricContext(nodeStartEvent);
    assertThat(metricsMap.isEmpty()).isFalse();
    assertThat(metricsMap.size()).isEqualTo(3);
    assertThat(metricsMap.get("accountId")).isEqualTo(AmbianceTestUtils.ACCOUNT_ID);
    assertThat(metricsMap.get("orgIdentifier")).isEqualTo(AmbianceTestUtils.ORG_ID);
    assertThat(metricsMap.get("projectIdentifier")).isEqualTo(AmbianceTestUtils.PROJECT_ID);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testMetricPrefix() {
    assertThat(nodeStartEventHandler.getMetricPrefix(nodeStartEvent)).isEqualTo("start_event");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractLogProperties() {
    Map<String, String> metricsMap = nodeStartEventHandler.extraLogProperties(nodeStartEvent);
    assertThat(metricsMap.isEmpty()).isFalse();
    assertThat(metricsMap.size()).isEqualTo(1);
    assertThat(metricsMap.get("eventType")).isEqualTo(NodeExecutionEventType.START.name());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractAmbiance() {
    assertThat(nodeStartEventHandler.extractAmbiance(nodeStartEvent)).isEqualTo(ambiance);
  }
}