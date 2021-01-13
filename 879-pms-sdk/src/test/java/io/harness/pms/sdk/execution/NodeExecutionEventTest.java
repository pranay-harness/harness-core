package io.harness.pms.sdk.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.joor.Reflect.on;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceBlockingStub;
import io.harness.pms.contracts.plan.NodeExecutionProtoServiceGrpc.NodeExecutionProtoServiceImplBase;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.sdk.PmsSdkTestBase;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.NodeExecutionEventListener;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.facilitator.sync.SyncFacilitator;
import io.harness.pms.sdk.core.registries.FacilitatorRegistry;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class NodeExecutionEventTest extends PmsSdkTestBase {
  private static final String NODE_EXECUTION_ID = generateUuid();

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private final NodeExecutionProtoServiceImplBase serviceImpl =
      mock(NodeExecutionProtoServiceImplBase.class, delegatesTo(new PmsNodeExecutionTestGrpcSevice() {}));

  @Mock private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private StepRegistry stepRegistry;

  private NodeExecutionEventListener eventListener;

  @Before
  public void setup() throws IOException {
    eventListener = new NodeExecutionEventListener(null);
    on(eventListener).set("engineObtainmentHelper", engineObtainmentHelper);
    on(eventListener).set("facilitatorRegistry", facilitatorRegistry);

    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName).directExecutor().addService(serviceImpl).build().start());
    ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    NodeExecutionProtoServiceBlockingStub stub = NodeExecutionProtoServiceGrpc.newBlockingStub(channel);
    PmsNodeExecutionService pmsNodeExecutionService = new PmsNodeExecutionServiceGrpcImpl();
    on(pmsNodeExecutionService).set("nodeExecutionProtoServiceBlockingStub", stub);
    on(pmsNodeExecutionService).set("stepRegistry", stepRegistry);
    on(eventListener).set("pmsNodeExecutionService", pmsNodeExecutionService);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldFacilitate() {
    assertThatCode(()
                       -> eventListener.onMessage(
                           NodeExecutionEvent.builder()
                               .eventType(NodeExecutionEventType.FACILITATE)
                               .nodeExecution(NodeExecutionProto.newBuilder()
                                                  .setUuid(NODE_EXECUTION_ID)
                                                  .setNode(PlanNodeProto.newBuilder()
                                                               .addFacilitatorObtainments(
                                                                   FacilitatorObtainment.newBuilder()
                                                                       .setType(SyncFacilitator.FACILITATOR_TYPE)
                                                                       .build())
                                                               .build())
                                                  .build())
                               .build()))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldAbort() {
    assertThatCode(
        ()
            -> eventListener.onMessage(
                NodeExecutionEvent.builder()
                    .eventType(NodeExecutionEventType.ABORT)
                    .nodeExecution(
                        NodeExecutionProto.newBuilder()
                            .setUuid(NODE_EXECUTION_ID)
                            .setNode(PlanNodeProto.newBuilder()
                                         .setStepType(StepType.newBuilder().setType("ABORTABLE_STEP").build())
                                         .build())
                            .build())
                    .build()))
        .doesNotThrowAnyException();
  }
}
