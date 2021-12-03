package io.harness.delegate.beans.connector.nexusconnector;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.validation.OneOfField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
@ApiModel("NexusUsernamePasswordAuth")
@JsonTypeName(NexusConstants.USERNAME_PASSWORD)
@OneOfField(fields = {"username", "usernameRef"})
@Schema(name = "NexusUsernamePasswordAuth",
    description = "This entity contains the details of the Nexus Username and Password")
public class NexusUsernamePasswordAuthDTO implements NexusAuthCredentialsDTO {
  String username;
  @ApiModelProperty(dataType = "string") @SecretReference SecretRefData usernameRef;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;
}
