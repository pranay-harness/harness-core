package io.harness.delegate.beans.connector.docker;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DockerUserNamePasswordDTO implements DockerAuthCredentialsDTO {
  @NotNull String username;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
