/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

import software.wings.sm.ContextElement;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
public class SkipStateExecutionData extends StateExecutionData {
  private String skipAssertionExpression;
  private String workflowId;

  @Builder
  public SkipStateExecutionData(String stateName, String stateType, Long startTs, Long endTs, ExecutionStatus status,
      String errorMsg, Integer waitInterval, ContextElement element, Map<String, Object> stateParams,
      Map<String, Object> templateVariable, String skipAssertionExpression, String workflowId) {
    super(stateName, stateType, startTs, endTs, status, errorMsg, waitInterval, element, stateParams, templateVariable);
    this.skipAssertionExpression = skipAssertionExpression;
    this.workflowId = workflowId;
  }

  public String getSkipAssertionExpression() {
    return skipAssertionExpression;
  }

  public void setSkipAssertionExpression(String skipAssertionExpression) {
    this.skipAssertionExpression = skipAssertionExpression;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "skipAssertionExpression",
        ExecutionDataValue.builder().displayName("Skip Condition").value(skipAssertionExpression).build());
    return executionDetails;
  }
}
