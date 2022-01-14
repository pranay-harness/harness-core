package io.harness.delegate.beans.connector.scm.genericgitconnector;

import io.harness.delegate.beans.connector.scm.GitConfigConstants;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(GitConfigConstants.SSH)
@Schema(name = "GitSSHAuthentication",
    description = "This contains details of the Generic Git authentication information used via SSH connections")
public class GitSSHAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("sshKeyRef")
  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  SecretRefData encryptedSshKey;
}
