/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.k8Connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@JsonTypeName(KubernetesConfigConstants.CLIENT_KEY_CERT)
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDP)
public class KubernetesClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData caCertRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientCertRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientKeyRef;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData clientKeyPassphraseRef;
  String clientKeyAlgo;
}
