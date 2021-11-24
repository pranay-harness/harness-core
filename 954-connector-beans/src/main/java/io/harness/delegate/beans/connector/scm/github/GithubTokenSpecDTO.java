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
@ApiModel("GithubTokenSpec")
@Schema(name = "GithubTokenSpec",
    description = "This contains details of the information such as references of token needed for Github API access")
public class GithubTokenSpecDTO implements GithubApiAccessSpecDTO {
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData tokenRef;
}
