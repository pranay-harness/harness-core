package io.harness.delegate.beans.connector.nexusconnector;

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
@ApiModel("NexusUsernamePasswordAuth")
@JsonTypeName(NexusConstants.USERNAME_PASSWORD)
public class NexusUsernamePasswordAuthDTO implements NexusAuthCredentialsDTO {
  @NotNull String username;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
