package io.harness.delegate.beans.connector.nexusconnector;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

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
@ApiModel("NexusUsernamePasswordAuth")
@JsonTypeName(NexusConstants.USERNAME_PASSWORD)
public class NexusUsernamePasswordAuthDTO implements NexusAuthCredentialsDTO {
  @NotNull String username;
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData passwordRef;
}
