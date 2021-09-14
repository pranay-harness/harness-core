/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.azure;

import software.wings.sm.StepExecutionSummary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureVMSSDeployExecutionSummary extends StepExecutionSummary {
  private String oldVirtualMachineScaleSetId;
  private String oldVirtualMachineScaleSetName;
  private String newVirtualMachineScaleSetId;
  private String newVirtualMachineScaleSetName;
}
