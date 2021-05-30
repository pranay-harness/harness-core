package io.harness.pms.sdk.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.joor.Reflect.on;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.PmsSdkTestBase;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionServiceImpl;
import io.harness.pms.sdk.core.execution.events.node.NodeExecutionEventListener;
import io.harness.pms.sdk.core.registries.FacilitatorRegistry;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.response.events.SdkResponseEventQueuePublisher;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeExecutionEventTest extends PmsSdkTestBase {
  private static final String NODE_EXECUTION_ID = generateUuid();

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Mock private EngineObtainmentHelper engineObtainmentHelper;
  @Mock private SdkResponseEventQueuePublisher sdkResponseEventQueuePublisher;
  @Mock private PmsGitSyncHelper pmsGitSyncHelper;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private StepRegistry stepRegistry;

  private NodeExecutionEventListener eventListener;

  @Before
  public void setup() throws IOException {
    eventListener = new NodeExecutionEventListener(null);
    on(eventListener).set("engineObtainmentHelper", engineObtainmentHelper);
    on(eventListener).set("facilitatorRegistry", facilitatorRegistry);
    on(eventListener).set("pmsGitSyncHelper", pmsGitSyncHelper);

    SdkNodeExecutionService sdkNodeExecutionService = new SdkNodeExecutionServiceImpl();
    on(sdkNodeExecutionService).set("stepRegistry", stepRegistry);
    on(sdkNodeExecutionService).set("sdkResponseEventQueuePublisher", sdkResponseEventQueuePublisher);
    on(eventListener).set("sdkNodeExecutionService", sdkNodeExecutionService);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFacilitate() {
    assertThatCode(
        ()
            -> eventListener.onMessage(
                NodeExecutionEvent.builder()
                    .eventType(NodeExecutionEventType.FACILITATE)
                    .nodeExecution(
                        NodeExecutionProto.newBuilder()
                            .setAmbiance(
                                Ambiance.newBuilder().setMetadata(ExecutionMetadata.newBuilder().build()).build())
                            .setUuid(NODE_EXECUTION_ID)
                            .setNode(PlanNodeProto.newBuilder()
                                         .addFacilitatorObtainments(
                                             FacilitatorObtainment.newBuilder()
                                                 .setType(FacilitatorType.newBuilder()
                                                              .setType(OrchestrationFacilitatorType.SYNC)
                                                              .build())
                                                 .build())
                                         .build())
                            .build())
                    .build()))
        .doesNotThrowAnyException();
  }
}
