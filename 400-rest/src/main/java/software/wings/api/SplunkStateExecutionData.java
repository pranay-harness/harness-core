/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import static org.apache.commons.lang3.StringUtils.abbreviate;

import software.wings.sm.StateExecutionData;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SplunkStateExecutionData extends StateExecutionData {
  private String query;
  private String response;
  private String assertionStatement;
  private String assertionStatus;

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "query", ExecutionDataValue.builder().displayName("Query").value(query).build());
    putNotNull(
        executionDetails, "response", ExecutionDataValue.builder().displayName("Response").value(response).build());
    putNotNull(executionDetails, "assertionStatement",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "query", ExecutionDataValue.builder().displayName("Query").value(query).build());
    putNotNull(executionDetails, "response",
        ExecutionDataValue.builder()
            .displayName("Response")
            .value(abbreviate(response, StateExecutionData.SUMMARY_PAYLOAD_LIMIT))
            .build());
    putNotNull(executionDetails, "assertionStatement",
        ExecutionDataValue.builder().displayName("Assertion").value(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        ExecutionDataValue.builder().displayName("Assertion Result").value(assertionStatus).build());
    return executionDetails;
  }
}
