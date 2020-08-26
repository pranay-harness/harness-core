package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName("OpenIdConnect")
public class KubernetesOpenIdConnectDTO extends KubernetesAuthCredentialDTO {
  @NotNull String oidcIssuerUrl;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcClientIdRef;

  @NotNull String oidcUsername;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcPasswordRef;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcSecretRef;

  String oidcScopes;
}
