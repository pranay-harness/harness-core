package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonTypeName("UsernamePassword")
public class KubernetesUserNamePasswordDTO extends KubernetesAuthCredentialDTO {
  @NotBlank String username;

  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData caCertRef;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
