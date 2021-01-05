package io.harness.delegate.beans.connector.artifactoryconnector;

import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel("ArtifactoryUsernamePasswordAuth")
@JsonTypeName(ArtifactoryConstants.USERNAME_PASSWORD)
public class ArtifactoryUsernamePasswordAuthDTO implements ArtifactoryAuthCredentialsDTO {
  @NotNull String username;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
