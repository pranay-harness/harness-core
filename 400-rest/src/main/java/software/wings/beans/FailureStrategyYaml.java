package software.wings.beans;

import io.harness.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class FailureStrategyYaml extends BaseYaml {
  private List<String> failureTypes = new ArrayList<>();
  private String executionScope;
  private String repairActionCode;
  private int retryCount;
  private List<Integer> retryIntervals;
  private String repairActionCodeAfterRetry;
  private FailureCriteria failureCriteria;
  private List<String> specificSteps = new ArrayList<>();
  private String actionAfterTimeout;
  private Long manualInterventionTimeout;

  @Builder
  public FailureStrategyYaml(List<String> failureTypes, String executionScope, String repairActionCode, int retryCount,
      List<Integer> retryIntervals, String repairActionCodeAfterRetry, FailureCriteria failureCriteria,
      List<String> specificSteps, String actionAfterTimeout, Long manualInterventionTimeout) {
    this.failureTypes = failureTypes;
    this.executionScope = executionScope;
    this.repairActionCode = repairActionCode;
    this.retryCount = retryCount;
    this.retryIntervals = retryIntervals;
    this.repairActionCodeAfterRetry = repairActionCodeAfterRetry;
    this.failureCriteria = failureCriteria;
    this.specificSteps = specificSteps;
    this.actionAfterTimeout = actionAfterTimeout;
    this.manualInterventionTimeout = manualInterventionTimeout;
  }
}
