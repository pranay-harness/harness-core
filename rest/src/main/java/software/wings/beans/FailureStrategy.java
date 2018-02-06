package software.wings.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by rishi on 10/31/16.
 */

public class FailureStrategy {
  @NotNull @Size(min = 1) private List<FailureType> failureTypes = new ArrayList<>();
  private ExecutionScope executionScope;
  private RepairActionCode repairActionCode;
  private int retryCount;
  private List<Integer> retryIntervals;
  private RepairActionCode repairActionCodeAfterRetry;
  private List<String> specificSteps;
  @Valid private FailureCriteria failureCriteria;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYaml {
    private List<String> failureTypes = new ArrayList<>();
    private String executionScope;
    private String repairActionCode;
    private int retryCount;
    private List<Integer> retryIntervals;
    private String repairActionCodeAfterRetry;
    private FailureCriteria failureCriteria;

    public static final class Builder {
      private List<String> failureTypes = new ArrayList<>();
      private String executionScope;
      private String repairActionCode;
      private int retryCount;
      private List<Integer> retryIntervals;
      private String repairActionCodeAfterRetry;
      private FailureCriteria failureCriteria;

      private Builder() {}

      public static Builder anYaml() {
        return new Builder();
      }

      public Builder withFailureTypes(List<String> failureTypes) {
        this.failureTypes = failureTypes;
        return this;
      }

      public Builder withExecutionScope(String executionScope) {
        this.executionScope = executionScope;
        return this;
      }

      public Builder withRepairActionCode(String repairActionCode) {
        this.repairActionCode = repairActionCode;
        return this;
      }

      public Builder withRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
      }

      public Builder withRetryIntervals(List<Integer> retryIntervals) {
        this.retryIntervals = retryIntervals;
        return this;
      }

      public Builder withRepairActionCodeAfterRetry(String repairActionCodeAfterRetry) {
        this.repairActionCodeAfterRetry = repairActionCodeAfterRetry;
        return this;
      }

      public Builder withFailureCriteria(FailureCriteria failureCriteria) {
        this.failureCriteria = failureCriteria;
        return this;
      }

      public Builder but() {
        return anYaml()
            .withFailureTypes(failureTypes)
            .withExecutionScope(executionScope)
            .withRepairActionCode(repairActionCode)
            .withRetryCount(retryCount)
            .withRetryIntervals(retryIntervals)
            .withRepairActionCodeAfterRetry(repairActionCodeAfterRetry)
            .withFailureCriteria(failureCriteria);
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setFailureTypes(failureTypes);
        yaml.setExecutionScope(executionScope);
        yaml.setRepairActionCode(repairActionCode);
        yaml.setRetryCount(retryCount);
        yaml.setRetryIntervals(retryIntervals);
        yaml.setRepairActionCodeAfterRetry(repairActionCodeAfterRetry);
        yaml.setFailureCriteria(failureCriteria);
        return yaml;
      }
    }
  }

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

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public List<Integer> getRetryIntervals() {
    return retryIntervals;
  }

  public void setRetryIntervals(List<Integer> retryIntervals) {
    this.retryIntervals = retryIntervals;
  }

  public RepairActionCode getRepairActionCodeAfterRetry() {
    return repairActionCodeAfterRetry;
  }

  public void setRepairActionCodeAfterRetry(RepairActionCode repairActionCodeAfterRetry) {
    this.repairActionCodeAfterRetry = repairActionCodeAfterRetry;
  }

  public List<String> getSpecificSteps() {
    return specificSteps;
  }

  public void setSpecificSteps(List<String> specificSteps) {
    this.specificSteps = specificSteps;
  }

  public FailureCriteria getFailureCriteria() {
    return failureCriteria;
  }

  public void setFailureCriteria(FailureCriteria failureCriteria) {
    this.failureCriteria = failureCriteria;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FailureStrategy that = (FailureStrategy) o;

    if (retryCount != that.retryCount) {
      return false;
    }
    if (failureTypes != null ? !failureTypes.equals(that.failureTypes) : that.failureTypes != null) {
      return false;
    }
    if (executionScope != that.executionScope) {
      return false;
    }
    if (repairActionCode != that.repairActionCode) {
      return false;
    }
    if (retryIntervals != null ? !retryIntervals.equals(that.retryIntervals) : that.retryIntervals != null) {
      return false;
    }
    if (repairActionCodeAfterRetry != that.repairActionCodeAfterRetry) {
      return false;
    }
    if (specificSteps != null ? !specificSteps.equals(that.specificSteps) : that.specificSteps != null) {
      return false;
    }
    if (failureCriteria != null ? !failureCriteria.equals(that.failureCriteria) : that.failureCriteria != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = failureTypes != null ? failureTypes.hashCode() : 0;
    result = 31 * result + (executionScope != null ? executionScope.hashCode() : 0);
    result = 31 * result + (repairActionCode != null ? repairActionCode.hashCode() : 0);
    result = 31 * result + retryCount;
    result = 31 * result + (retryIntervals != null ? retryIntervals.hashCode() : 0);
    result = 31 * result + (repairActionCodeAfterRetry != null ? repairActionCodeAfterRetry.hashCode() : 0);
    result = 31 * result + (specificSteps != null ? specificSteps.hashCode() : 0);
    result = 31 * result + (failureCriteria != null ? failureCriteria.hashCode() : 0);
    return result;
  }

  public static final class FailureStrategyBuilder {
    private List<FailureType> failureTypes = new ArrayList<>();
    private ExecutionScope executionScope;
    private RepairActionCode repairActionCode;
    private int retryCount;
    private List<Integer> retryIntervals;
    private RepairActionCode repairActionCodeAfterRetry;
    private FailureCriteria failureCriteria;

    private FailureStrategyBuilder() {}

    public static FailureStrategyBuilder aFailureStrategy() {
      return new FailureStrategyBuilder();
    }

    public FailureStrategyBuilder addFailureTypes(FailureType failureType) {
      failureTypes.add(failureType);
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

    public FailureStrategyBuilder withRetryCount(int retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    public FailureStrategyBuilder withRetryIntervals(List<Integer> retryIntervals) {
      this.retryIntervals = retryIntervals;
      return this;
    }

    public FailureStrategyBuilder withRepairActionCodeAfterRetry(RepairActionCode repairActionCodeAfterRetry) {
      this.repairActionCodeAfterRetry = repairActionCodeAfterRetry;
      return this;
    }

    public FailureStrategyBuilder withFailureCriteria(FailureCriteria failureCriteria) {
      this.failureCriteria = failureCriteria;
      return this;
    }

    public FailureStrategy build() {
      FailureStrategy failureStrategy = new FailureStrategy();
      failureStrategy.setFailureTypes(failureTypes);
      failureStrategy.setExecutionScope(executionScope);
      failureStrategy.setRepairActionCode(repairActionCode);
      failureStrategy.setRetryCount(retryCount);
      failureStrategy.setRetryIntervals(retryIntervals);
      failureStrategy.setRepairActionCodeAfterRetry(repairActionCodeAfterRetry);
      failureStrategy.setFailureCriteria(failureCriteria);
      return failureStrategy;
    }
  }
}
