package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel("ArtifactoryUsernamePasswordAuth")
@JsonTypeName(ArtifactoryConstants.USERNAME_PASSWORD)
@OneOfField(fields = {"username", "usernameRef"})
public class ArtifactoryUsernamePasswordAuthDTO implements ArtifactoryAuthCredentialsDTO {
  String username;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData usernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
