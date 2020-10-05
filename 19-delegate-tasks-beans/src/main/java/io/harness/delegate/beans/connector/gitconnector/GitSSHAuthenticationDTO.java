package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(GitConfigConstants.SSH)
public class GitSSHAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("sshKeyReference") String encryptedSshKey;
}
