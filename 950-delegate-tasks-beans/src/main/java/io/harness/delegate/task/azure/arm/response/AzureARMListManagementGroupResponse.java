/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.azure.arm.response;

import io.harness.delegate.beans.azure.ManagementGroupData;
import io.harness.delegate.task.azure.arm.AzureARMTaskResponse;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AzureARMListManagementGroupResponse extends AzureARMTaskResponse {
  private List<ManagementGroupData> mngGroups;

  @Builder
  public AzureARMListManagementGroupResponse(List<ManagementGroupData> mngGroups, String errorMsg) {
    super(errorMsg);
    this.mngGroups = mngGroups;
  }
}
