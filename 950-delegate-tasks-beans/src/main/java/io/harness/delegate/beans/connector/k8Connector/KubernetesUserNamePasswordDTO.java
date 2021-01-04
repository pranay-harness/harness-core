package io.harness.delegate.beans.connector.k8Connector;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(KubernetesConfigConstants.USERNAME_PASSWORD)
@OneOfField(fields = {"username", "usernameRef"})
public class KubernetesUserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  String username;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData usernameRef;
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData passwordRef;
}
