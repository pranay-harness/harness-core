package io.harness.delegate.beans.connector.docker;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class DockerUserNamePasswordDTO implements DockerAuthCredentialsDTO {
  @NotNull String username;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
