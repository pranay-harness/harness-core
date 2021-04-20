package io.harness.delegate.beans.connector.helm;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OneOfField(fields = {"username", "usernameRef"})
public class HttpHelmUsernamePasswordDTO implements HttpHelmAuthCredentialsDTO {
  String username;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData usernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
