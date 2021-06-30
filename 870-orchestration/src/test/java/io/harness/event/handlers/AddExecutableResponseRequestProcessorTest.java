package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import java.util.EnumSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class AddExecutableResponseRequestProcessorTest {
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @InjectMocks AddExecutableResponseRequestProcessor addExecutableResponseEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    verifyNoMoreInteractions(nodeExecutionService);
    verifyNoMoreInteractions(waitNotifyEngine);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventNoopStatus() {
    AddExecutableResponseRequest request =
        AddExecutableResponseRequest.newBuilder().setStatus(Status.ASYNC_WAITING).build();
    addExecutableResponseEventHandler.handleEvent(
        SdkResponseEventProto.newBuilder()
            .setAddExecutableResponseRequest(request)
            .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .setNodeExecutionId("id")
            .build());
    verify(nodeExecutionService).updateStatusWithOps(eq("id"), eq(Status.ASYNC_WAITING), any(), any());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventWithStatus() {
    AddExecutableResponseRequest request =
        AddExecutableResponseRequest.newBuilder().setStatus(Status.SUCCEEDED).build();
    addExecutableResponseEventHandler.handleEvent(
        SdkResponseEventProto.newBuilder()
            .setNodeExecutionId("id")
            .setAddExecutableResponseRequest(request)
            .setSdkResponseEventType(SdkResponseEventType.ADD_EXECUTABLE_RESPONSE)
            .build());
    verify(nodeExecutionService)
        .updateStatusWithOps(eq("id"), eq(Status.SUCCEEDED), any(), eq(EnumSet.noneOf(Status.class)));
  }
}