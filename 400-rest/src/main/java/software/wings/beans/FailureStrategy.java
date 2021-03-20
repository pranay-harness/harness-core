package software.wings.beans;

import io.harness.exception.FailureType;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.interrupts.RepairActionCode;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class FailureStrategy {
  @NotNull @Size(min = 1, message = "should not be empty") private List<FailureType> failureTypes;
  private ExecutionScope executionScope;
  private RepairActionCode repairActionCode;
  private int retryCount;
  private List<Integer> retryIntervals;
  private RepairActionCode repairActionCodeAfterRetry;
  @Valid private FailureCriteria failureCriteria;
  private List<String> specificSteps;
  private Long manualInterventionTimeout;
  private ExecutionInterruptType actionAfterTimeout;
}
