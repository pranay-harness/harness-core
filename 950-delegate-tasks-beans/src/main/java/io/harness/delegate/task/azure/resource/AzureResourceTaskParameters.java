/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.resource;

import io.harness.delegate.task.azure.AzureTaskParameters;
import io.harness.delegate.task.azure.resource.operation.AzureResourceOperation;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AzureResourceTaskParameters extends AzureTaskParameters {
  @NotNull private AzureResourceOperation resourceOperation;

  @Builder
  public AzureResourceTaskParameters(String appId, String accountId, AzureResourceOperation operationRequest) {
    super(appId, accountId);
    this.resourceOperation = operationRequest;
  }
}
