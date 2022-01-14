package io.harness.delegate.beans.connector.k8Connector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(KubernetesConfigConstants.USERNAME_PASSWORD)
@OneOfField(fields = {"username", "usernameRef"})
@Schema(name = "KubernetesUserNamePassword", description = "This contains kubernetes username password details")
public class KubernetesUserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  String username;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData usernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
