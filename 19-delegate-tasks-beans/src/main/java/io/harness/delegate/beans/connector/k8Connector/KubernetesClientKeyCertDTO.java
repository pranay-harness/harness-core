package io.harness.delegate.beans.connector.k8Connector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(KubernetesConfigConstants.CLIENT_KEY_CERT)
public class KubernetesClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData caCertRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientCertRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientKeyRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientKeyPassphraseRef;
  String clientKeyAlgo;
}
