package software.wings.beans;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by rishi on 10/31/16.
 */
public class FailureStrategy {
  @NotNull @Size(min = 1) private List<FailureType> failureTypes = new ArrayList<>();
  private ExecutionScope executionScope;
  private RepairActionCode repairActionCode;

  public List<FailureType> getFailureTypes() {
    return failureTypes;
  }

  public void setFailureTypes(List<FailureType> failureTypes) {
    this.failureTypes = failureTypes;
  }

  public ExecutionScope getExecutionScope() {
    return executionScope;
  }

  public void setExecutionScope(ExecutionScope executionScope) {
    this.executionScope = executionScope;
  }

  public RepairActionCode getRepairActionCode() {
    return repairActionCode;
  }

  public void setRepairActionCode(RepairActionCode repairActionCode) {
    this.repairActionCode = repairActionCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    FailureStrategy that = (FailureStrategy) o;

    if (failureTypes != null ? !failureTypes.equals(that.failureTypes) : that.failureTypes != null)
      return false;
    if (executionScope != that.executionScope)
      return false;
    return repairActionCode == that.repairActionCode;
  }

  @Override
  public int hashCode() {
    int result = failureTypes != null ? failureTypes.hashCode() : 0;
    result = 31 * result + (executionScope != null ? executionScope.hashCode() : 0);
    result = 31 * result + (repairActionCode != null ? repairActionCode.hashCode() : 0);
    return result;
  }

  public static final class FailureStrategyBuilder {
    private List<FailureType> failureTypes = new ArrayList<>();
    private ExecutionScope executionScope;
    private RepairActionCode repairActionCode;

    private FailureStrategyBuilder() {}

    public static FailureStrategyBuilder aFailureStrategy() {
      return new FailureStrategyBuilder();
    }

    public FailureStrategyBuilder addFailureTypes(FailureType failureType) {
      this.failureTypes.add(failureType);
      return this;
    }

    public FailureStrategyBuilder withExecutionScope(ExecutionScope executionScope) {
      this.executionScope = executionScope;
      return this;
    }

    public FailureStrategyBuilder withRepairActionCode(RepairActionCode repairActionCode) {
      this.repairActionCode = repairActionCode;
      return this;
    }

    public FailureStrategy build() {
      FailureStrategy failureStrategy = new FailureStrategy();
      failureStrategy.setFailureTypes(failureTypes);
      failureStrategy.setExecutionScope(executionScope);
      failureStrategy.setRepairActionCode(repairActionCode);
      return failureStrategy;
    }
  }
}
