/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
