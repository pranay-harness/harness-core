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
@ApiModel("GithubAppSpec")
@Schema(name = "GithubAppSpec",
    description = "This contains details of the Github API access credentials Specs such as references of private key")
public class GithubAppSpecDTO implements GithubApiAccessSpecDTO {
  @NotNull String installationId;
  @NotNull String applicationId;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData privateKeyRef;
}
