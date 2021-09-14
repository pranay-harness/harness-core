/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.azure.manager;

import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("azureVMSSAllPhaseRollbackData")
public class AzureVMSSAllPhaseRollbackData implements SweepingOutput {
  public static final String AZURE_VMSS_ALL_PHASE_ROLLBACK = "Azure VMSS all phase rollback";
  boolean allPhaseRollbackDone;

  @Override
  public String getType() {
    return "azureVMSSAllPhaseRollbackData";
  }
}
