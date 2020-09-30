package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class GitSSHAuthenticationDTO extends GitAuthenticationDTO {
  @JsonProperty("sshKeyReference") String encryptedSshKey;
}
