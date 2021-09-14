/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.k8Connector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(KubernetesConfigConstants.OPENID_CONNECT)
@OneOfField(fields = {"oidcUsername", "oidcUsernameRef"})
public class KubernetesOpenIdConnectDTO extends KubernetesAuthCredentialDTO {
  @NotBlank String oidcIssuerUrl;
  String oidcUsername;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData oidcUsernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcClientIdRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcPasswordRef;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData oidcSecretRef;
  String oidcScopes;
}
