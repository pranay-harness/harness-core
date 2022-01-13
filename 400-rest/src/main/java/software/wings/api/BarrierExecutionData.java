/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
