/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSDeployTaskResponse implements AzureVMSSTaskResponse {
  private List<AzureVMInstanceData> vmInstancesAdded;
  private List<AzureVMInstanceData> vmInstancesExisting;
}
