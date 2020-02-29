package software.wings.api;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import software.wings.sm.ContextElement;
import software.wings.sm.StateExecutionData;

import java.util.Map;

public class SkipStateExecutionData extends StateExecutionData {
  private String skipAssertionExpression;

  @Builder
  public SkipStateExecutionData(String stateName, String stateType, Long startTs, Long endTs, ExecutionStatus status,
      String errorMsg, Integer waitInterval, ContextElement element, Map<String, Object> stateParams,
      Map<String, Object> templateVariable, String skipAssertionExpression) {
    super(stateName, stateType, startTs, endTs, status, errorMsg, waitInterval, element, stateParams, templateVariable);
    this.skipAssertionExpression = skipAssertionExpression;
  }

  public String getSkipAssertionExpression() {
    return skipAssertionExpression;
  }

  public void setSkipAssertionExpression(String skipAssertionExpression) {
    this.skipAssertionExpression = skipAssertionExpression;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "skipAssertionExpression",
        ExecutionDataValue.builder().displayName("Skip Condition").value(skipAssertionExpression).build());
    return executionDetails;
  }
}
