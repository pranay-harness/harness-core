package io.harness.delegate.beans.connector.helm;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OneOfField(fields = {"username", "usernameRef"})
@Schema(name = "HttpHelmUsernamePassword", description = "This contains http helm username password details")
public class HttpHelmUsernamePasswordDTO implements HttpHelmAuthCredentialsDTO {
  String username;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData usernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
