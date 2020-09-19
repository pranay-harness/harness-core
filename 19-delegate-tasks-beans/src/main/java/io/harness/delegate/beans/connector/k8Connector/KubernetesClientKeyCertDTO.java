package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonTypeName("ClientKeyCert")
public class KubernetesClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData caCertRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientCertRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientKeyRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData clientKeyPassphraseRef;
  String clientKeyAlgo;
}
