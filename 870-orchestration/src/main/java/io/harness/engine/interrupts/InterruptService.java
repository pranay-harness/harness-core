package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionCheck;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;
import io.harness.pms.contracts.interrupts.InterruptType;

import java.util.List;
import javax.validation.Valid;

@OwnedBy(PIPELINE)
public interface InterruptService {
  List<Interrupt> fetchActiveInterrupts(String planExecutionId);

  List<Interrupt> fetchActiveInterruptsForNodeExecution(String planExecutionId, String nodeExecutionId);

  List<Interrupt> fetchActiveInterruptsForNodeExecutionByType(
      String planExecutionId, String nodeExecutionId, InterruptType interruptType);

  List<Interrupt> fetchAllInterrupts(String planExecutionId);

  Interrupt markProcessed(String interruptId, State interruptState);

  Interrupt markProcessing(String interruptId);

  List<Interrupt> fetchActivePlanLevelInterrupts(String planExecutionId);

  ExecutionCheck checkInterruptsPreInvocation(String planExecutionId, String nodeExecutionId);

  Interrupt save(@Valid Interrupt interrupt);

  Interrupt get(String interruptId);

  long closeActiveInterrupts(String planExecutionId);
}
