package io.harness.delegate.beans.connector.scm.github;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GithubUsernameToken")
@Schema(name = "GithubUsernameToken",
    description = "This contains details of the Github credentials Specs such as references of username and token")
public class GithubUsernameTokenDTO implements GithubHttpCredentialsSpecDTO {
  String username;
  @SecretReference @ApiModelProperty(dataType = "string") SecretRefData usernameRef;
  @NotNull @ApiModelProperty(dataType = "string") @SecretReference SecretRefData tokenRef;
}
