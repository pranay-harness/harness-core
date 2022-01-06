/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
