package io.harness.delegate.beans.connector.k8Connector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class KubernetesUserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  @NotBlank String username;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
