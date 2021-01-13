package io.harness.delegate.beans.connector.scm.genericgitconnector;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(GitConfigConstants.SSH)
public class GitSSHAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("sshKeyReference")
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  SecretRefData encryptedSshKey;
}
