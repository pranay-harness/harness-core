/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.newrelic;

import software.wings.api.ExecutionDataValue;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NewRelicMarkerExecutionData extends StateExecutionData {
  private String payload;
  private String evaluatedPayload;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    return getExecutionDetails();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "errorMsg", ExecutionDataValue.builder().displayName("Message").value(getErrorMsg()).build());
    putNotNull(executionDetails, "payload", ExecutionDataValue.builder().displayName("Payload").value(payload).build());

    putNotNull(executionDetails, "evaluatedPayload",
        ExecutionDataValue.builder().displayName("Evaluated Payload").value(evaluatedPayload).build());

    return executionDetails;
  }
}
