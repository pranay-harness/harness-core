/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api;

import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class BarrierExecutionData extends StateExecutionData {
  @Getter @Setter private String identifier;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "identifier",
        ExecutionDataValue.builder().displayName("Identifier").value(identifier).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "identifier",
        ExecutionDataValue.builder().displayName("Identifier").value(identifier).build());
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    BarrierStepExecutionSummary barrierStepExecutionSummary = new BarrierStepExecutionSummary();
    populateStepExecutionSummary(barrierStepExecutionSummary);
    barrierStepExecutionSummary.setIdentifier(identifier);
    return barrierStepExecutionSummary;
  }
}
