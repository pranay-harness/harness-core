package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.Interrupt.State;
import org.springframework.data.repository.CrudRepository;

import java.util.EnumSet;
import java.util.List;

@OwnedBy(CDC)
@HarnessRepo
public interface InterruptRepository extends CrudRepository<Interrupt, String> {
  List<Interrupt> findByPlanExecutionIdAndStateInOrderByCreatedAtDesc(
      String planExecutionId, EnumSet<State> registered);

  List<Interrupt> findByPlanExecutionIdOrderByCreatedAtDesc(String planExecutionId);

  List<Interrupt> findByPlanExecutionIdAndStateInAndTypeInOrderByCreatedAtDesc(
      String planExecutionId, EnumSet<State> registered, EnumSet<ExecutionInterruptType> planLevelInterrupts);
}
