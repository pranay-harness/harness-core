package io.harness.engine.interrupts;

import io.harness.annotation.HarnessRepo;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;
import org.springframework.data.repository.CrudRepository;

import java.util.EnumSet;
import java.util.List;

@HarnessRepo
public interface InterruptRepository extends CrudRepository<Interrupt, String> {
  List<Interrupt> findByPlanExecutionIdAndStateInOrderByCreatedAtDesc(
      String planExecutionId, EnumSet<State> registered);

  List<Interrupt> findByPlanExecutionIdOrderByCreatedAtDesc(String planExecutionId);

  List<Interrupt> findByPlanExecutionIdAndStateInAndTypeInOrderByCreatedAtDesc(
      String planExecutionId, EnumSet<State> registered, EnumSet<ExecutionInterruptType> planLevelInterrupts);
}
