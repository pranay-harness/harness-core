/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static software.wings.common.InfrastructureConstants.QUEUING_RC_NAME;

import software.wings.sm.StateExecutionData;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class ResourceConstraintExecutionData extends StateExecutionData {
  @Getter @Setter private String resourceConstraintName;
  @Getter @Setter private int resourceConstraintCapacity;
  @Getter @Setter private String unit;
  @Getter @Setter private int usage;
  @Getter @Setter private int alreadyAcquiredPermits;
  @Getter @Setter private boolean harnessOwned;

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "Name",
        ExecutionDataValue.builder().displayName("Name").value(resourceConstraintName).build());
    putNotNull(executionDetails, "Unit", ExecutionDataValue.builder().displayName("Unit").value(unit).build());
    putNotNull(executionDetails, "Harness Owned",
        ExecutionDataValue.builder().displayName("Harness Owned").value(harnessOwned).build());

    if (!QUEUING_RC_NAME.equals(resourceConstraintName)) {
      putNotNull(executionDetails, "Capacity",
          ExecutionDataValue.builder().displayName("Capacity").value(resourceConstraintCapacity).build());
      putNotNull(executionDetails, "Usage", ExecutionDataValue.builder().displayName("Usage").value(usage).build());
      putNotNull(executionDetails, "alreadyAcquiredPermits",
          ExecutionDataValue.builder().displayName("Already Acquired Permits").value(alreadyAcquiredPermits).build());
    }
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "Name",
        ExecutionDataValue.builder().displayName("Name").value(resourceConstraintName).build());
    putNotNull(executionDetails, "Unit", ExecutionDataValue.builder().displayName("Unit").value(unit).build());
    putNotNull(executionDetails, "Harness Owned",
        ExecutionDataValue.builder().displayName("Harness Owned").value(harnessOwned).build());

    if (!QUEUING_RC_NAME.equals(resourceConstraintName)) {
      putNotNull(executionDetails, "Capacity",
          ExecutionDataValue.builder().displayName("Capacity").value(resourceConstraintCapacity).build());
      putNotNull(executionDetails, "Usage", ExecutionDataValue.builder().displayName("Usage").value(usage).build());
      putNotNull(executionDetails, "alreadyAcquiredPermits",
          ExecutionDataValue.builder().displayName("Already Acquired Permits").value(alreadyAcquiredPermits).build());
    }
    return executionDetails;
  }

  @Override
  public StepExecutionSummary getStepExecutionSummary() {
    ResourceConstraintStepExecutionSummary executionSummary = new ResourceConstraintStepExecutionSummary();
    populateStepExecutionSummary(executionSummary);
    executionSummary.setResourceConstraintName(resourceConstraintName);
    executionSummary.setResourceConstraintCapacity(resourceConstraintCapacity);
    executionSummary.setUnit(unit);
    executionSummary.setUsage(usage);
    executionSummary.setAlreadyAcquiredPermits(alreadyAcquiredPermits);
    return executionSummary;
  }
}
