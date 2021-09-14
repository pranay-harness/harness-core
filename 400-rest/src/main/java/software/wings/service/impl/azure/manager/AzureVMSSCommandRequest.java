/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.azure.manager;

import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.azure.request.AzureVMSSTaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.delegatetasks.delegatecapability.CapabilityHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSCommandRequest implements TaskParameters, ExecutionCapabilityDemander {
  private AzureConfigDTO azureConfigDTO;
  private List<EncryptedDataDetail> azureConfigEncryptionDetails;
  private AzureVMSSTaskParameters azureVMSSTaskParameters;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    Set<ExecutionCapability> executionCapabilities = new HashSet<>(
        CapabilityHelper.generateDelegateCapabilities(azureConfigDTO, azureConfigEncryptionDetails, maskingEvaluator));
    return new ArrayList<>(executionCapabilities);
  }
}
