/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.azure;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("VMAuth")
public class AzureVMAuthDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  private String userName;

  private String secretRefFieldName = "secretRef";
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretRef;

  @JsonProperty("type") private io.harness.delegate.beans.azure.AzureVMAuthType azureVmAuthType;

  @Builder
  public AzureVMAuthDTO(
      SecretRefData secretRef, io.harness.delegate.beans.azure.AzureVMAuthType azureVmAuthType, String userName) {
    this.secretRef = secretRef;
    this.azureVmAuthType = azureVmAuthType;
    this.userName = userName;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      io.harness.expression.ExpressionEvaluator maskingEvaluator) {
    return new ArrayList<>();
  }
}
