package io.harness.delegate.beans.connector.artifactoryconnector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
@JsonTypeName(ArtifactoryConstants.usernamePassword)
@ApiModel("ArtifactoryUsernamePasswordAuth")
public class ArtifactoryUsernamePasswordAuthDTO implements ArtifactoryAuthCredentialsDTO {
  @NotNull String username;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
