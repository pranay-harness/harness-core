/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.interrupts.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.contracts.execution.Status.RUNNING;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptHandler;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.helpers.RetryHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.ExecutionModeUtils;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;
import io.harness.pms.execution.utils.StatusUtils;

import com.google.inject.Inject;

@OwnedBy(CDC)
public class RetryInterruptHandler implements InterruptHandler {
  @Inject private RetryHelper retryHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private InterruptService interruptService;

  @Override
  public Interrupt registerInterrupt(Interrupt interrupt) {
    Interrupt savedInterrupt = validateAndSave(interrupt);
    return handleInterruptForNodeExecution(savedInterrupt, savedInterrupt.getNodeExecutionId());
  }

  private Interrupt validateAndSave(Interrupt interrupt) {
    if (isEmpty(interrupt.getNodeExecutionId())) {
      throw new InvalidRequestException("NodeExecutionId Cannot be empty for RETRY interrupt");
    }
    NodeExecution nodeExecution = nodeExecutionService.get(interrupt.getNodeExecutionId());
    if (!StatusUtils.retryableStatuses().contains(nodeExecution.getStatus())) {
      throw new InvalidRequestException(
          "NodeExecution is not in a retryable status. Current Status: " + nodeExecution.getStatus());
    }

    if (ExecutionModeUtils.isParentMode(nodeExecution.getMode())) {
      throw new InvalidRequestException("Node Retry is supported only for Leaf Nodes");
    }
    interrupt.setState(State.PROCESSING);
    return interruptService.save(interrupt);
  }

  @Override
  public Interrupt handleInterrupt(Interrupt interrupt) {
    throw new UnsupportedOperationException("Please use handleInterrupt for handling retries");
  }

  @Override
  public Interrupt handleInterruptForNodeExecution(Interrupt interrupt, String nodeExecutionId) {
    retryHelper.retryNodeExecution(
        interrupt.getNodeExecutionId(), interrupt.getParameters(), interrupt.getUuid(), interrupt.getInterruptConfig());
    planExecutionService.updateStatus(interrupt.getPlanExecutionId(), RUNNING);
    return interruptService.markProcessed(interrupt.getUuid(), State.PROCESSED_SUCCESSFULLY);
  }
}
