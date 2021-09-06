package io.harness.engine.executions.node;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.rule.Owner;
import io.harness.timeout.TimeoutInstance;

import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeExecutionTimeoutCallbackTest extends OrchestrationTestBase {
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private InterruptManager interruptManager;

  private final String planExecutionId = generateUuid();
  private final String nodeExecutionId = generateUuid();

  private NodeExecutionTimeoutCallback callback;

  @Before
  public void setUp() {
    callback = new NodeExecutionTimeoutCallback(planExecutionId, nodeExecutionId);
    Reflect.on(callback).set("nodeExecutionService", nodeExecutionService);
    Reflect.on(callback).set("interruptManager", interruptManager);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnTimeoutWhenNodeExecutionNull() {
    when(nodeExecutionService.get(nodeExecutionId)).thenReturn(null);

    callback.onTimeout(TimeoutInstance.builder().build());

    verify(nodeExecutionService).get(nodeExecutionId);
    verify(nodeExecutionService, never()).update(any(), any());
    verify(interruptManager, never()).register(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnTimeoutWhenNodeExecutionIsNotInFinalizableStatus() {
    NodeExecution nodeExecution = NodeExecution.builder().status(Status.FAILED).build();
    when(nodeExecutionService.get(nodeExecutionId)).thenReturn(nodeExecution);

    callback.onTimeout(TimeoutInstance.builder().build());

    verify(nodeExecutionService).get(nodeExecutionId);
    verify(nodeExecutionService, never()).update(any(), any());
    verify(interruptManager, never()).register(any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnTimeoutWhenIsParentMode() {
    NodeExecution nodeExecution =
        NodeExecution.builder().uuid(nodeExecutionId).status(Status.RUNNING).mode(ExecutionMode.CHILD).build();
    when(nodeExecutionService.get(nodeExecutionId)).thenReturn(nodeExecution);

    callback.onTimeout(TimeoutInstance.builder().uuid(generateUuid()).build());

    ArgumentCaptor<InterruptPackage> interruptPackageArgumentCaptor = ArgumentCaptor.forClass(InterruptPackage.class);
    verify(interruptManager).register(interruptPackageArgumentCaptor.capture());

    InterruptPackage interruptPackage = interruptPackageArgumentCaptor.getValue();
    assertThat(interruptPackage.getInterruptType()).isEqualTo(InterruptType.EXPIRE_ALL);

    verify(nodeExecutionService).get(nodeExecutionId);
    verify(nodeExecutionService).update(any(), any());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestOnTimeout() {
    NodeExecution nodeExecution =
        NodeExecution.builder().uuid(nodeExecutionId).status(Status.RUNNING).mode(ExecutionMode.TASK).build();
    when(nodeExecutionService.get(nodeExecutionId)).thenReturn(nodeExecution);

    callback.onTimeout(TimeoutInstance.builder().uuid(generateUuid()).build());

    ArgumentCaptor<InterruptPackage> interruptPackageArgumentCaptor = ArgumentCaptor.forClass(InterruptPackage.class);
    verify(interruptManager).register(interruptPackageArgumentCaptor.capture());

    InterruptPackage interruptPackage = interruptPackageArgumentCaptor.getValue();
    assertThat(interruptPackage.getInterruptType()).isEqualTo(InterruptType.MARK_EXPIRED);

    verify(nodeExecutionService).get(nodeExecutionId);
    verify(nodeExecutionService).update(any(), any());
  }
}