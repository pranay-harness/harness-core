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

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(KubernetesConfigConstants.OPENID_CONNECT)
@OneOfField(fields = {"oidcUsername", "oidcUsernameRef"})
public class KubernetesOpenIdConnectDTO extends KubernetesAuthCredentialDTO {
  @NotNull String oidcIssuerUrl;
  String oidcUsername;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData oidcUsernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcClientIdRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData oidcPasswordRef;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData oidcSecretRef;
  String oidcScopes;
}
