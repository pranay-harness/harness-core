package io.harness.delegate.beans.connector.k8Connector;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@JsonTypeName(KubernetesConfigConstants.CLIENT_KEY_CERT)
@EqualsAndHashCode(callSuper = false)
public class KubernetesClientKeyCertDTO extends KubernetesAuthCredentialDTO {
  @ApiModelProperty(dataType = "string") @SecretReference @Pattern(regexp = SECRET_REF_PATTERN) SecretRefData caCertRef;
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData clientCertRef;
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData clientKeyRef;
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData clientKeyPassphraseRef;
  String clientKeyAlgo;
}
