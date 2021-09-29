package io.harness.engine.interrupts.callback;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.InterruptService;
import io.harness.interrupts.Interrupt;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.interrupts.IssuedBy;
import io.harness.pms.contracts.interrupts.ManualIssuer;
import io.harness.rule.Owner;
import io.harness.waiter.StringNotifyResponseData;

import com.google.common.collect.ImmutableMap;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class FailureInterruptCallbackTest extends OrchestrationTestBase {
  @Mock InterruptService interruptService;
  @Mock NodeExecutionService nodeExecutionService;
  @Mock OrchestrationEngine orchestrationEngine;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestNotify() {
    String nodeExecutionId = generateUuid();
    String interruptId = generateUuid();
    FailureInterruptCallback failureInterruptCallback =
        FailureInterruptCallback.builder()
            .interruptId(interruptId)
            .interruptType(InterruptType.CUSTOM_FAILURE)
            .nodeExecutionId(nodeExecutionId)
            .interruptConfig(
                InterruptConfig.newBuilder()
                    .setIssuedBy(
                        IssuedBy.newBuilder()
                            .setManualIssuer(ManualIssuer.newBuilder().setIdentifier("MANUAL").setType("").build())
                            .build())
                    .build())
            .build();
    Reflect.on(failureInterruptCallback).set("interruptService", interruptService);
    Reflect.on(failureInterruptCallback).set("nodeExecutionService", nodeExecutionService);
    Reflect.on(failureInterruptCallback).set("orchestrationEngine", orchestrationEngine);

    failureInterruptCallback.notify(
        ImmutableMap.of(generateUuid(), StringNotifyResponseData.builder().data("SOMEDATA").build()));

    verify(nodeExecutionService).update(eq(nodeExecutionId), any());
    verify(orchestrationEngine).concludeNodeExecution(any(), eq(Status.FAILED));
    verify(interruptService).markProcessed(eq(interruptId), eq(Interrupt.State.PROCESSED_SUCCESSFULLY));
  }
}