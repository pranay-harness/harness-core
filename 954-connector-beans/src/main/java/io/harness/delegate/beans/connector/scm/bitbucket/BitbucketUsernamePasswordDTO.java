package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

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
@ApiModel("BitbucketUsernamePassword")
@OneOfField(fields = {"username", "usernameRef"})
@Schema(name = "BitbucketUsernamePassword",
    description =
        "This contains details of the Bitbucket credentials Specs such as references of username and password")
public class BitbucketUsernamePasswordDTO implements BitbucketHttpCredentialsSpecDTO {
  String username;
  @SecretReference @ApiModelProperty(dataType = "string") SecretRefData usernameRef;
  @NotNull @SecretReference @ApiModelProperty(dataType = "string") SecretRefData passwordRef;
}
